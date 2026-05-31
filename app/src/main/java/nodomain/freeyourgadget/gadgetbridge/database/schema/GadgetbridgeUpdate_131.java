/*  Copyright (C) 2026 Thomas Kuehne

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.database.schema;

import android.database.sqlite.SQLiteDatabase;

import nodomain.freeyourgadget.gadgetbridge.database.DBUpdateScript;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericMetricSampleDao;
import nodomain.freeyourgadget.gadgetbridge.model.MetricSample;

public class GadgetbridgeUpdate_131 implements DBUpdateScript {

    private static void fixTimeStampOfGenericMetricSample(SQLiteDatabase db) {
        // GenericMetricSamples were originally stored with epoc seconds instead
        // of the usual epoc milli seconds
        // epoc 31536000000 ms = epoc 31536000 s = year 1971
        String statement = String.format("UPDATE %1$s SET %2$s = %2$s * 1000 WHERE %2$s < 31536000000",
                GenericMetricSampleDao.TABLENAME,
                GenericMetricSampleDao.Properties.Timestamp.columnName);
        db.execSQL(statement);
    }

    private static void copyTrainingLoadAcute(SQLiteDatabase db) {
        // one-off copy existing data so that vizualizations
        // and migration can be tested before removing old code paths
        final String sql;
        sql = "INSERT OR IGNORE INTO GENERIC_METRIC_SAMPLE(TIMESTAMP, DEVICE_ID, USER_ID, METRIC_TYPE ,METRIC_SCORE, METRIC_EXTRA) "
                + "SELECT TIMESTAMP, DEVICE_ID, USER_ID, " + MetricSample.Metric.GENERIC_TRAINING_LOAD_ACUTE.dbId + ", VALUE, NULL "
                + "FROM GENERIC_TRAINING_LOAD_ACUTE_SAMPLE";
        db.execSQL(sql);
    }

    private static void copyTrainingLoadChronic(SQLiteDatabase db) {
        // one-off copy existing data so that vizualizations
        // and migration can be tested before removing old code paths
        final String sql;
        sql = "INSERT OR IGNORE INTO GENERIC_METRIC_SAMPLE(TIMESTAMP, DEVICE_ID, USER_ID, METRIC_TYPE ,METRIC_SCORE, METRIC_EXTRA) "
                + "SELECT TIMESTAMP, DEVICE_ID, USER_ID, " + MetricSample.Metric.GENERIC_TRAINING_LOAD_CHRONIC.dbId + ", VALUE, NULL "
                + "FROM GENERIC_TRAINING_LOAD_CHRONIC_SAMPLE";
        db.execSQL(sql);
    }

    private static void copyRestingMetabolicRate(SQLiteDatabase db) {
        // one-off copy existing data so that vizualizations
        // and migration can be tested before removing old code paths
        final String sql;
        sql = "INSERT OR IGNORE INTO GENERIC_METRIC_SAMPLE(TIMESTAMP, DEVICE_ID, USER_ID, METRIC_TYPE ,METRIC_SCORE, METRIC_EXTRA) "
                + "SELECT TIMESTAMP, DEVICE_ID, USER_ID, " + MetricSample.Metric.GENERIC_RESTING_METABOLIC_RATE.dbId + ", RESTING_METABOLIC_RATE, NULL "
                + "FROM GARMIN_RESTING_METABOLIC_RATE_SAMPLE";
        db.execSQL(sql);
    }

    @Override
    public void upgradeSchema(final SQLiteDatabase db) {
        fixTimeStampOfGenericMetricSample(db);
        copyTrainingLoadAcute(db);
        copyTrainingLoadChronic(db);
        copyRestingMetabolicRate(db);
    }

    @Override
    public void downgradeSchema(final SQLiteDatabase db) {
    }
}
