/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
// A new entry's task id doubles as its daily health logEntryId, which is a UUIDv7 like the ones scanners generate.
Ext4.define('NBRI_EHR.panel.DailyHealthDataEntryPanel', {
    extend: 'EHR.panel.TaskDataEntryPanel',

    initComponent: function(){
        this.taskId = this.taskId || LABKEY.ActionURL.getParameter('taskid') || LABKEY.ActionURL.getParameter('taskId') || this.generateUUIDv7();
        this.callParent();
    },

    // RFC 9562: 48-bit Unix millisecond timestamp, version 7, variant 10, the rest random
    generateUUIDv7: function(){
        var bytes = new Uint8Array(16);
        window.crypto.getRandomValues(bytes);

        var ms = Date.now();
        for (var i = 5; i >= 0; i--){
            bytes[i] = ms % 256;
            ms = Math.floor(ms / 256);
        }
        bytes[6] = (bytes[6] & 0x0f) | 0x70;
        bytes[8] = (bytes[8] & 0x3f) | 0x80;

        var hex = Array.prototype.map.call(bytes, function(b){
            return (b < 16 ? '0' : '') + b.toString(16);
        }).join('');
        return [hex.substr(0, 8), hex.substr(8, 4), hex.substr(12, 4), hex.substr(16, 4), hex.substr(20)].join('-');
    }
});
