/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
s.*,
co.qcstate.label AS obsStatus
FROM
    (SELECT
    s1.*,
    timestampadd('SQL_TSI_MINUTE', ((s1.hours * 60) + s1.minutes), s1.origDate) AS date,
    ((s1.hours * 60) + s1.minutes) AS timeOffset

    FROM (
        SELECT
        t1.lsid,
        t1.dataset,
        t1.id AS animalid,
        coalesce(ft.hourofday, ((hour(t1.date) * 100) + minute(t1.date))) AS time,
        (coalesce(ft.hourofday, (hour(t1.date) * 100)) / 100) AS hours,

        CASE WHEN ft.hourofday IS NOT NULL
        THEN (((ft.hourofday / 100.0) - floor(ft.hourofday / 100)) * 100)
        ELSE minute(t1.date)
        END AS minutes,

        dr.date AS origDate,

        t1.frequency.meaning AS frequency,
        t1.date AS startDate,
        timestampdiff('SQL_TSI_DAY', cast(t1.dateOnly AS timestamp), dr.dateOnly) + 1 AS daysElapsed,
        t1.enddate,
        t1.category,
        t1.area,
        t1.performedby,
        t1.remark,
        t1.caseid.objectid AS caseid,
        t1.taskid,
        t1.type,
        t1.objectid,

        t1.qcstate
        FROM nbri_ehr.dateRange dr
        JOIN

        -- order by category to replace string with Daily Obs
        (SELECT * FROM study.observation_order ORDER BY category) t1
        ON (dr.dateOnly >= t1.dateOnly AND (dr.dateOnly <= t1.enddate OR t1.enddate IS NULL) AND
          --technically the first day of the treatment is day 1, not day 0
        ((mod(CAST(timestampdiff('SQL_TSI_DAY', CAST(t1.dateOnly AS timestamp), dr.dateOnly) AS integer), t1.frequency.intervalindays) = 0 AND t1.frequency.intervalindays IS NOT NULL AND t1.frequency.dayofweek IS NULL))
        AND (t1.frequency.weekDays IS NULL OR LOCATE(CAST(dr.DayOfWeek AS VARCHAR), t1.frequency.weekDays) > 0)
        )
        LEFT JOIN ehr_lookups.treatment_frequency_times ft ON ft.frequency = t1.frequency.meaning
        WHERE t1.date IS NOT NULL

    ) s1
) s
LEFT JOIN study.clinical_observations co ON co.scheduledDate IS NOT NULL AND s.date = co.scheduledDate AND co.orderId = s.objectid