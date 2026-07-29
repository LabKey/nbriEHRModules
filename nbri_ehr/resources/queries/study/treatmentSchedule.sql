/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

SELECT
d.id,
d_alias.alias AS Name,
d.calculated_status,
s.*,
s.objectid AS treatmentid,
drug.treatmentStatus,
s.objectid || '-pkSeparator-' || s.date AS primaryKey -- -pkSeparator- is used to separate the two parts of the primary key in RecordTreatmentButton.js
FROM study.demographics d
JOIN(
    SELECT
    s1.*,
    timestampadd('SQL_TSI_MINUTE', ((s1.hours * 60) + s1.minutes), s1.origDate) AS date,
    ((s1.hours * 60) + s1.minutes) AS timeOffset
    FROM (
        SELECT
        t1.lsid,
        t1.objectid,
        t1.dataset,
        t1.id AS animalid,
        
        COALESCE(ft.hourofday, ((hour(t1.date) * 100) + minute(t1.date))) AS time,
        (COALESCE(ft.hourofday, (hour(t1.date) * 100)) / 100) AS hours,
        CASE
            WHEN (ft.hourofday IS NOT NULL) THEN (((ft.hourofday / 100.0) - floor(ft.hourofday / 100)) * 100)
            ELSE minute(t1.date)
        END AS minutes,
        dr.date AS origDate,
        t1.category,
        t1.frequency.meaning AS frequency,
        t1.date AS startDate,
        timestampdiff('SQL_TSI_DAY', cast(t1.dateOnly AS timestamp), dr.dateOnly) + 1 AS daysElapsed,
        t1.enddate,
        t1.code,
        t1.volume,
        t1.vol_units,
        t1.concentration,
        t1.conc_units,
        t1.amount,
        t1.amount_units,
        t1.amountWithUnits,
        t1.amountAndVolume,
        t1.dosage,
        t1.dosage_units,
        t1.route,
        IFDEFINED(t1.reason) AS reason,
        t1.performedby,
        t1.remark,
        t1.caseid,
        t1.orderedby,
        t1.qcstate
        FROM nbri_ehr.dateRange dr
        JOIN study."Treatment Orders" t1 ON (dr.dateOnly >= t1.dateOnly AND
              --technically the first day of the treatment is day 1, not day 0
            ((mod(CAST(timestampdiff('SQL_TSI_DAY', CAST(t1.dateOnly AS timestamp), dr.dateOnly) AS integer), t1.frequency.intervalindays) = 0 AND t1.frequency.intervalindays IS NOT NULL AND t1.frequency.dayofweek IS NULL ))
            AND (t1.frequency.weekDays IS NULL OR LOCATE(CAST(dr.DayOfWeek AS VARCHAR), t1.frequency.weekDays) > 0)
        )
        LEFT JOIN ehr_lookups.treatment_frequency_times ft ON ft.frequency = t1.frequency.meaning
        --NOTE: if we run this report on a future interval, we want to include those treatments
        WHERE t1.date IS NOT NULL
    
    ) s1

) s ON (s.animalid = d.id) 
LEFT JOIN (
    SELECT treatmentid, IFDEFINED(scheduledDate) AS scheduledDate, MAX(qcstate.label) AS treatmentStatus
    FROM study.drug
    GROUP BY treatmentid, IFDEFINED(scheduledDate)
) drug ON s.objectid = drug.treatmentid AND s.date = drug.scheduledDate
LEFT JOIN (SELECT Id, GROUP_CONCAT(alias, ', ') alias FROM alias WHERE category.title = 'Name' GROUP BY Id) d_alias ON d.id = d_alias.id
WHERE (d.lastDayatCenter IS NULL OR d.lastDayAtCenter > s.enddate OR s.enddate IS NULL)
    AND s.date >= s.startDate AND (s.enddate IS NULL OR s.date <= s.enddate)