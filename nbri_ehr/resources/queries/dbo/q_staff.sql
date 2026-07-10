/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    STAFF_ID as staffId,
    EMAIL_ADDRESS as email,
    trim(STAFF_FIRST_NAME) as firstName,
    trim(STAFF_LAST_NAME) as lastName,
    STAFF_MIDDLE_NAME as middleName,
    STAFF_NUMBER as displayName,
    HIRE_DATE as hireDate,
    LAST_EMPLOY_DATE as lastEmployDate,
    OFFICE_TELEPHONE as officePhone,
    OFFICE_PHONE_EXT as officePhoneExt,
    OFFICE_FAX as officeFax,
    HOME_TELEPHONE as homePhone,
    BEEPER_TELEPHONE as beeperPhone,
    CELLULAR_TELEPHONE as cellPhone,
    EMERGENCY_CONTACT as emergencyContact,
    EMERGENCY_TELEPHONE as emergencyPhone,
    HOME_ADDRESS1 as homeAddress1,
    HOME_ADDRESS2 as homeAddress2,
    HOME_CITY as homeCity,
    HOME_STATE_PROV as homeState,
    HOME_ZIP as homeZip,
    HOME_ZIP_EXT as homeZipExt,
    HOME_COUNTRY as homeCountry,
    OFFICE_ADDRESS1 as officeAddress1,
    OFFICE_ADDRESS2 as officeAddress2,
    OFFICE_CITY as officeCity,
    OFFICE_STATE_PROV as officeState,
    OFFICE_ZIP as officeZip,
    OFFICE_ZIP_EXT as officeZipExt,
    OFFICE_COUNTRY as officeCountry,
    BIRTHDATE as birthDate,
    HS_NAME_DESC as hsName,
    COLLEGE_GRAD_DATE as collegeGradDate,
    COLLEGE_DEGREE as collegeDegree,
    COLLEGE_MAJOR as collegeMajor,
    COLLEGE_NAME_DESC as collegeName,
    SUPERVISOR_STAFF_ID as supervisor,
    POSITION_NAME as positionName,
    POSITION_DESCRIPTION as positionDesc,
    ACTIVE_YN as active
FROM STAFF



