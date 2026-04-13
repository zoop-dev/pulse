/*  Copyright (C) 2017-2026 Carsten Pfeiffer, Daniele Gobbetti, José Rebelo, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;
import java.util.Objects;

public class ActivityPoint {
    private Date time;
    private GPSCoordinate location;
    private int heartRate;
    private float speed = -1;
    private int stride = -1;
    private int stepLength = -1;
    private int cadence = -1;
    private float power = -1;
    private float respiratoryRate = -1;
    private double depth = -1;
    private double temperature = -273;
    private double distance = -1.0;
    private float bodyEnergy = -1.0f;
    private float stamina = -1.0f;
    private float cnsToxicity = -1.0f;
    private float n2Load = -1.0f;
    private double altitude = GPSCoordinate.UNKNOWN_ALTITUDE;

    // e.g. to describe a pause during the activity
    private @Nullable String description;

    public ActivityPoint() {
    }

    public ActivityPoint(Date time) {
        this.time = time;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(@NonNull Date time) {
        this.time = time;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Nullable
    public GPSCoordinate getLocation() {
        return location;
    }

    public void setLocation(@Nullable GPSCoordinate location) {
        this.location = location;
    }

    /// @see ActivitySummaryEntries#UNIT_METERS
    public double getAltitude() {
        if (altitude > GPSCoordinate.UNKNOWN_ALTITUDE) {
            return altitude;
        }
        if (location != null) {
            return location.getAltitude();
        }
        return GPSCoordinate.UNKNOWN_ALTITUDE;
    }

    /// 24/7 energy level
    ///
    /// @see ActivitySummaryEntries#UNIT_PERCENTAGE
    public float getBodyEnergy() {
        return bodyEnergy;
    }

    /// endurance capacity when doing a specific activity
    ///
    /// @see ActivitySummaryEntries#UNIT_PERCENTAGE
    public float getStamina() {
        return stamina;
    }

    /// central nervous system (CNS) toxicity (e.g. Diving)
    ///
    /// @see ActivitySummaryEntries#UNIT_PERCENTAGE
    public float getCnsToxicity() {
        return cnsToxicity;
    }

    /// nitrogen (N2) tissue load (e.g. Diving)
    /// @see ActivitySummaryEntries#UNIT_PERCENTAGE
    public float getN2Load() {
        return n2Load;
    }

    /// @see ActivitySummaryEntries#UNIT_METERS
    public double getDistance() {
        return distance;
    }

    /// @see ActivitySummaryEntries#UNIT_METERS
    public void setDistance(double distanceInMeter) {
        distance = distanceInMeter;
    }

    /// @see ActivitySummaryEntries#UNIT_BPM
    public int getHeartRate() {
        return heartRate;
    }

    /// @see ActivitySummaryEntries#UNIT_BPM
    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }

    /// @see ActivitySummaryEntries#UNIT_METERS_PER_SECOND
    public float getSpeed() {
        return speed;
    }

    /// @see ActivitySummaryEntries#UNIT_METERS_PER_SECOND
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /// distance from one foot landing to the same foot landing again
    /// @see ActivitySummaryEntries#UNIT_MM
    public int getStride() {
        return stride;
    }

    /// distance from one foot landing to the opposite foot landing
    /// @see ActivitySummaryEntries#UNIT_MM
    public int getStepLength() {
        return stepLength;
    }

    public int getCadence() {
        return cadence;
    }

    public void setCadence(final int cadence) {
        this.cadence = cadence;
    }

    /// @see ActivitySummaryEntries#UNIT_WATT
    public float getPower() {
        return power;
    }

    /// @see ActivitySummaryEntries#UNIT_WATT
    public void setPower(final float power) {
        this.power = power;
    }

    /// @see ActivitySummaryEntries#UNIT_BREATHS_PER_MIN
    public float getRespiratoryRate() {
        return respiratoryRate;
    }

    /// @see ActivitySummaryEntries#UNIT_BREATHS_PER_MIN
    public void setRespiratoryRate(final float respiratoryRate) {
        this.respiratoryRate = respiratoryRate;
    }

    /// @see ActivitySummaryEntries#UNIT_CELSIUS
    public double getTemperature() {
        return temperature;
    }

    /// @see ActivitySummaryEntries#UNIT_CELSIUS
    public void setTemperature(final double temperature) {
        this.temperature = temperature;
    }

    /// @see ActivitySummaryEntries#UNIT_METERS
    public double getDepth() {
        return depth;
    }

    /// @see ActivitySummaryEntries#UNIT_METERS
    public void setDepth(final double depth) {
        this.depth = depth;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ActivityPoint that)) return false;
        return heartRate == that.heartRate &&
                Float.compare(speed, that.speed) == 0 &&
                stride == that.stride &&
                cadence == that.cadence &&
                Float.compare(power, that.power) == 0 &&
                Float.compare(respiratoryRate, that.respiratoryRate) == 0 &&
                Double.compare(depth, that.depth) == 0 &&
                Double.compare(temperature, that.temperature) == 0 &&
                Objects.equals(time, that.time) &&
                Objects.equals(location, that.location) &&
                Objects.equals(description, that.description) &&
                Double.compare(distance, that.distance) == 0 &&
                Double.compare(altitude, that.altitude) == 0 &&
                Float.compare(bodyEnergy, that.bodyEnergy) == 0 &&
                Float.compare(stamina, that.stamina) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, location, heartRate, speed, stride, cadence, power, respiratoryRate, depth, temperature, description, distance, altitude, bodyEnergy, stamina);
    }

    public static class Builder {
        private Date date;

        @Nullable
        private String description;

        private GPSCoordinate location;
        private double latitude = Double.NaN;
        private double longitude = Double.NaN;
        private double altitude = Double.NaN;
        private double hdop = Double.NaN;
        private double distance = Double.NaN;

        private int heartRate = Integer.MIN_VALUE;
        private float speed = Float.NaN;
        private int stride = Integer.MIN_VALUE;
        private int stepLength = Integer.MIN_VALUE;
        private int cadence = Integer.MIN_VALUE;
        private float power = Float.NaN;
        private float respiratoryRate = Float.NaN;
        private double depth = Double.NaN;
        private double temperature = Double.NaN;
        private float bodyEnergy = Float.NaN;
        private float stamina = Float.NaN;
        private float cnsToxicity = Float.NaN;
        private float n2Load = Float.NaN;

        public Builder() {
        }

        public Builder(long epocMilliSeconds) {
            date = new Date(epocMilliSeconds);
        }

        public Builder(@NonNull Long epocMilliSeconds) {
            date = new Date(epocMilliSeconds);
        }

        public Builder(@NonNull Date date) {
            this.date = date;
        }

        public long getTime() {
            return date.getTime();
        }

        public void setTime(long unixEpocMilliSeconds) {
            date = new Date(unixEpocMilliSeconds);
        }

        public void setTime(@NonNull Date date) {
            this.date = date;
        }

        public void setLatitude(@Nullable final Double latitude) {
            this.latitude = (latitude == null) ? Double.NaN : latitude;
        }

        public void setLatitude(final double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(@Nullable final Double longitude) {
            this.longitude = (longitude == null) ? Double.NaN : longitude;
        }

        public void setLongitude(final double longitude) {
            this.longitude = longitude;
        }

        /// @see ActivitySummaryEntries#UNIT_METERS
        public double getAltitude() {
            return altitude;
        }

        /// @see ActivitySummaryEntries#UNIT_METERS
        public void setAltitude(@Nullable final Number altitude) {
            this.altitude = (altitude == null) ? Double.NaN : altitude.doubleValue();
        }

        /// @see ActivitySummaryEntries#UNIT_METERS
        public void setAltitude(final double altitude) {
            this.altitude = altitude;
        }

        /// @see ActivitySummaryEntries#UNIT_BPM
        public int getHeartRate() {
            return heartRate;
        }

        /// @see ActivitySummaryEntries#UNIT_BPM
        public void setHeartRate(@Nullable final Number heartRate) {
            this.heartRate = (heartRate == null) ? Integer.MIN_VALUE : heartRate.intValue();
        }

        /// @see ActivitySummaryEntries#UNIT_BPM
        public void setHeartRate(final int heartRate) {
            this.heartRate = heartRate;
        }

        /// @see ActivitySummaryEntries#UNIT_METERS_PER_SECOND
        public float getSpeed() {
            return speed;
        }

        /// @see ActivitySummaryEntries#UNIT_METERS_PER_SECOND
        public void setSpeed(@Nullable final Number speed) {
            this.speed = (speed == null) ? Float.NaN : speed.floatValue();
        }

        /// @see ActivitySummaryEntries#UNIT_METERS_PER_SECOND
        public void setSpeed(final float speed) {
            this.speed = speed;
        }

        /// @see ActivitySummaryEntries#UNIT_MM
        public int getStride() {
            return stride;
        }

        /// distance from one foot landing to the same foot landing again
        /// @see ActivitySummaryEntries#UNIT_MM
        public void setStride(@Nullable Number stride) {
            this.stride = (stride == null) ? Integer.MIN_VALUE : stride.intValue();
        }

        /// distance from one foot landing to the same foot landing again
        /// @see ActivitySummaryEntries#UNIT_MM
        public void setStride(int stride) {
            this.stride = stride;
        }

        public int getCadence() {
            return cadence;
        }

        public void setCadence(@Nullable final Number cadence) {
            this.cadence = (cadence == null) ? Integer.MIN_VALUE : cadence.intValue();
        }

        public void setCadence(final int cadence) {
            this.cadence = cadence;
        }

        /// @see ActivitySummaryEntries#UNIT_WATT
        public float getPower() {
            return power;
        }

        /// @see ActivitySummaryEntries#UNIT_WATT
        public void setPower(@Nullable final Number power) {
            this.power = (power == null) ? Integer.MIN_VALUE : power.intValue();
        }

        /// @see ActivitySummaryEntries#UNIT_WATT
        public void setPower(final int power) {
            this.power = power;
        }

        /// @see ActivitySummaryEntries#UNIT_BREATHS_PER_MIN
        public float getRespiratoryRate() {
            return respiratoryRate;
        }

        /// @see ActivitySummaryEntries#UNIT_BREATHS_PER_MIN
        public void setRespiratoryRate(@Nullable final Number respiratoryRate) {
            this.respiratoryRate = (respiratoryRate == null) ? Float.NaN : respiratoryRate.floatValue();
        }

        /// @see ActivitySummaryEntries#UNIT_BREATHS_PER_MIN
        public void setRespiratoryRate(final float respiratoryRate) {
            this.respiratoryRate = respiratoryRate;
        }

        /// @see ActivitySummaryEntries#UNIT_METERS
        public double getDepth() {
            return depth;
        }

        /// @see ActivitySummaryEntries#UNIT_METERS
        public void setDepth(@Nullable final Number depth) {
            this.depth = (depth == null) ? Double.NaN : depth.doubleValue();
        }

        /// @see ActivitySummaryEntries#UNIT_METERS
        public void setDepth(final double depth) {
            this.depth = depth;
        }

        public void setLocation(@Nullable final GPSCoordinate location) {
            this.location = location;
        }

        /// @see ActivitySummaryEntries#UNIT_CELSIUS
        public double getTemperature() {
            return temperature;
        }

        /// @see ActivitySummaryEntries#UNIT_CELSIUS
        public void setTemperature(@Nullable final Number temperature) {
            this.temperature = (temperature == null) ? Double.NaN : temperature.doubleValue();
        }

        /// @see ActivitySummaryEntries#UNIT_CELSIUS
        public void setTemperature(final double temperature) {
            this.temperature = temperature;
        }

        /// horizontal dilution of precision
        ///
        /// @see ActivitySummaryEntries#UNIT_METERS
        public void setHdop(@Nullable final Number hdop) {
            this.hdop = (hdop == null) ? Double.NaN : hdop.doubleValue();
        }

        /// horizontal dilution of precision
        ///
        /// @see ActivitySummaryEntries#UNIT_METERS
        public void setHdop(final double hdop) {
            this.hdop = hdop;
        }

        /// total traveled distance
        ///
        /// @see ActivitySummaryEntries#UNIT_METERS
        public void setDistance(@Nullable final Number distance) {
            this.distance = (distance == null) ? Double.NaN : distance.doubleValue();
        }

        /// total traveled distance
        ///
        /// @see ActivitySummaryEntries#UNIT_METERS
        public void setDistance(final double distance) {
            this.distance = distance;
        }

        /// e.g. to describe a pause during the activity
        public void setDescription(@Nullable String description) {
            this.description = description;
        }

        /// 24/7 energy level
        /// @see ActivitySummaryEntries#UNIT_PERCENTAGE
        public void setBodyEnergy(@Nullable Number bodyEnergy) {
            this.bodyEnergy = (bodyEnergy == null) ? Float.NaN : bodyEnergy.floatValue();
        }

        /// 24/7 energy level
        /// @see ActivitySummaryEntries#UNIT_PERCENTAGE
        public void setBodyEnergy(float bodyBattery) {
            this.bodyEnergy = bodyBattery;
        }

        /// endurance capacity when doing a specific activity
        /// @see ActivitySummaryEntries#UNIT_PERCENTAGE
        public void setStamina(@Nullable Number stamina) {
            this.stamina = (stamina == null) ? Float.NaN : stamina.floatValue();
        }

        /// endurance capacity when doing a specific activity
        /// @see ActivitySummaryEntries#UNIT_PERCENTAGE
        public void setStamina(float stamina) {
            this.stamina = stamina;
        }

        /// distance from one foot landing to the opposite foot landing
        /// @see ActivitySummaryEntries#UNIT_MM
        public void setStepLength(@Nullable Number stepLength){
            this.stepLength = (stepLength == null) ? Integer.MIN_VALUE : stepLength.intValue();
        }

        /// distance from one foot landing to the opposite foot landing
        /// @see ActivitySummaryEntries#UNIT_MM
        public void setStepLength(int stepLength){
            this.stepLength = stepLength;
        }

        /// central nervous system (CNS) toxicity (e.g. Diving)
        /// @see ActivitySummaryEntries#UNIT_PERCENTAGE
        public void setCnsToxicity(@Nullable Number cnsToxicity) {
            this.cnsToxicity = (cnsToxicity == null) ? Float.NaN : cnsToxicity.floatValue();
        }

        /// central nervous system (CNS) toxicity (e.g. Diving)
        /// @see ActivitySummaryEntries#UNIT_PERCENTAGE
        public void setCnsToxicity(float cnsToxicity) {
            this.cnsToxicity = cnsToxicity;
        }

        /// nitrogen (N2) tissue load (e.g. Diving)
        /// @see ActivitySummaryEntries#UNIT_PERCENTAGE
        public void setN2Load(@Nullable Number n2Load) {
            this.n2Load = (n2Load == null) ? Float.NaN : n2Load.floatValue();
        }

        /// nitrogen (N2) tissue load (e.g. Diving)
        /// @see ActivitySummaryEntries#UNIT_PERCENTAGE
        public void setN2Load(float n2Load) {
            this.n2Load = n2Load;
        }


        public ActivityPoint build() {
            final ActivityPoint activityPoint = new ActivityPoint(date);
            if (location != null) {
                activityPoint.setLocation(location);
            } else if (!(Double.isNaN(latitude) || Double.isNaN(longitude))) {
                final GPSCoordinate loc;
                if (altitude > GPSCoordinate.UNKNOWN_ALTITUDE) {
                    loc = new GPSCoordinate(longitude, latitude, altitude);
                } else {
                    loc = new GPSCoordinate(longitude, latitude);
                }
                if (hdop > 0.0) {
                    loc.setHdop(hdop);
                }
                activityPoint.setLocation(loc);
            } else if (altitude > GPSCoordinate.UNKNOWN_ALTITUDE) {
                activityPoint.altitude = altitude;
            }

            if (heartRate > Integer.MIN_VALUE) {
                activityPoint.setHeartRate(heartRate);
            }
            if (!Float.isNaN(speed)) {
                activityPoint.setSpeed(speed);
            }
            if (cadence > Integer.MIN_VALUE) {
                activityPoint.setCadence(cadence);
            }
            if (stride > Integer.MIN_VALUE) {
                activityPoint.stride = stride;
            }
            if (!Float.isNaN(power)) {
                activityPoint.setPower(power);
            }
            if (!Float.isNaN(respiratoryRate)) {
                activityPoint.setRespiratoryRate(respiratoryRate);
            }
            if (!Double.isNaN(depth)) {
                activityPoint.setDepth(depth);
            }
            if (!Double.isNaN(temperature)) {
                activityPoint.setTemperature(temperature);
            }
            if (description != null && description.length() > 0) {
                activityPoint.setDescription(description);
            }
            if (!Double.isNaN(distance)) {
                activityPoint.setDistance(distance);
            }
            if(!Float.isNaN(bodyEnergy)){
                activityPoint.bodyEnergy = bodyEnergy;
            }
            if(!Float.isNaN(stamina)){
                activityPoint.stamina = stamina;
            }
            if(stepLength > Integer.MIN_VALUE){
                activityPoint.stepLength = stepLength;
            }
            if(!Float.isNaN(cnsToxicity)){
                activityPoint.cnsToxicity = cnsToxicity;
            }
            if(!Float.isNaN(n2Load)){
                activityPoint.n2Load = n2Load;
            }

            return activityPoint;
        }
    }
}
