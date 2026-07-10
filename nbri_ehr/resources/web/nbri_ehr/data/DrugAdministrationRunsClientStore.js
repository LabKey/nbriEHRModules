/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('NBRI_EHR.data.DrugAdministrationRunsClientStore', {
    extend: 'EHR.data.DrugAdministrationRunsClientStore',

    // This override adds a special case for frequency to align formulary frequency.rowid with data frequency.meaning
    onRecordUpdate: function (record, modifiedFieldNames) {
        if (record.get('code')) {
            modifiedFieldNames = modifiedFieldNames || [];

            if (modifiedFieldNames.indexOf('code') == -1) {
                return;
            }

            if (!this.formularyStore) {
                LDK.Utils.logToServer({
                    message: 'Unable to find formulary store in DrugAdministrationRunsClientStore'
                });
                console.error('Unable to find formulary store in DrugAdministrationRunsClientStore');

                return;
            }

            var values = this.formularyStore.getFormularyValues(record.get('code'));
            if (!Ext4.Object.isEmpty(values)) {
                var params = {};

                for (var fieldName in this.fieldMap) {
                    if (!this.getFields().get(fieldName)) {
                        continue;
                    }

                    if (modifiedFieldNames.indexOf(this.fieldMap[fieldName]) != -1) {
                        continue;
                    }

                    let def = values[fieldName];

                    // Special case for frequency to align the formulary frequency rowid with data stored as the meaning
                    if (fieldName.toLowerCase() === 'frequency')
                        def = values['frequency/meaning']

                    if (Ext4.isDefined(def)) {
                        params[this.fieldMap[fieldName]] = def;
                    }
                }

                if (!LABKEY.Utils.isEmptyObj(params)) {
                    record.beginEdit();
                    record.set(params);
                    record.endEdit(true);
                }
            }
        }
    }
});