/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

/*
 * Reports animals whose demographics birth/death values disagree with the birth and deaths event records that are
 * supposed to feed them.
 *
 * demographics.birth and demographics.death are denormalized copies, written by trigger scripts rather than derived,
 * so anything that bypasses those triggers -- ETL loads, admin edits, a partially failed save -- leaves them stale.
 * The framework reads the demographics copy (not the event record) for age, lastDayAtCenter and status, so drift here
 * is silently wrong data everywhere those appear.
 *
 * Only public (Completed) event records count as backing, and draft demographics records are excluded, so rows still
 * in data entry are not reported. Each animal yields at most one row; the three drift columns are independent and can
 * be filtered separately in the grid.
 *
 * Note: on a container populated by ETL or legacy import, "no completed birth record" can be the common case rather
 * than the exception. Filter birthDrift to triage.
 */

SELECT * FROM (
    SELECT
        ids.Id,
        dem.calculated_status,

        dem.birth AS demographicsBirth,
        b.date AS birthRecordDate,
        CASE
            WHEN dem.Id IS NULL THEN 'Event record exists with no demographics record'
            WHEN dem.birth IS NULL AND b.Id IS NOT NULL THEN 'Birth record exists but demographics birth is empty'
            WHEN dem.birth IS NOT NULL AND b.Id IS NULL THEN 'Demographics birth is set with no completed birth record'
            WHEN CAST(dem.birth AS DATE) <> CAST(b.date AS DATE) THEN 'Birth dates disagree'
        END AS birthDrift,

        dem.death AS demographicsDeath,
        d.date AS deathRecordDate,
        CASE
            WHEN dem.Id IS NULL THEN 'Event record exists with no demographics record'
            WHEN dem.death IS NULL AND d.Id IS NOT NULL THEN 'Death record exists but demographics death is empty'
            WHEN dem.death IS NOT NULL AND d.Id IS NULL THEN 'Demographics death is set with no completed death record'
            WHEN CAST(dem.death AS DATE) <> CAST(d.date AS DATE) THEN 'Death dates disagree'
        END AS deathDrift,

        CASE
            WHEN dem.Id IS NULL THEN NULL
            WHEN d.Id IS NOT NULL AND (dem.calculated_status IS NULL OR dem.calculated_status <> 'Dead')
                THEN 'Completed death record but status is not Dead'
            WHEN d.Id IS NULL AND dem.calculated_status = 'Dead'
                THEN 'Status is Dead with no completed death record'
        END AS statusDrift,

        dem.QCState.PublicData AS demographicsIsPublic

    FROM (
        SELECT Id FROM study.demographics
        UNION
        SELECT Id FROM study.birth
        UNION
        SELECT Id FROM study.deaths
    ) ids
    LEFT JOIN study.demographics dem ON ids.Id = dem.Id
    LEFT JOIN (SELECT Id, date FROM study.birth WHERE QCState.PublicData = true) b ON ids.Id = b.Id
    LEFT JOIN (SELECT Id, date FROM study.deaths WHERE QCState.PublicData = true) d ON ids.Id = d.Id
) t
WHERE (t.demographicsIsPublic = true OR t.demographicsIsPublic IS NULL)
  AND (t.birthDrift IS NOT NULL OR t.deathDrift IS NOT NULL OR t.statusDrift IS NOT NULL)
