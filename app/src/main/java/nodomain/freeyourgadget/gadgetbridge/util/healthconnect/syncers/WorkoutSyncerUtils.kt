/*  Copyright (C) 2025 Gideon Zenz

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
package nodomain.freeyourgadget.gadgetbridge.util.healthconnect.syncers

import androidx.health.connect.client.records.ExerciseSessionRecord
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind

/**
 * Shared utilities for workout syncing.
 */
internal object WorkoutSyncerUtils {
    /**
     * Maps Gadgetbridge ActivityKind to Health Connect ExerciseSessionRecord type.
     */
    fun mapActivityKindToExerciseType(activityKind: ActivityKind): Int {
        return when (activityKind) {
            ActivityKind.RUNNING, ActivityKind.OUTDOOR_RUNNING, ActivityKind.CROSS_COUNTRY_RUNNING, ActivityKind.TRAIL_RUN -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            ActivityKind.WALKING, ActivityKind.OUTDOOR_WALKING, ActivityKind.RACE_WALKING -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
            ActivityKind.SWIMMING -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL
            ActivityKind.CYCLING, ActivityKind.OUTDOOR_CYCLING -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
            ActivityKind.TREADMILL, ActivityKind.INDOOR_RUNNING -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL
            ActivityKind.INDOOR_CYCLING, ActivityKind.DYNAMIC_CYCLE, ActivityKind.SPINNING -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY
            ActivityKind.SWIMMING_OPENWATER, ActivityKind.FINSWIMMING -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER
            ActivityKind.ELLIPTICAL_TRAINER, ActivityKind.CROSS_TRAINER, ActivityKind.AIR_WALKER -> ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL
            ActivityKind.JUMP_ROPING, ActivityKind.BUNGEE_JUMPING -> ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS
            ActivityKind.YOGA -> ExerciseSessionRecord.EXERCISE_TYPE_YOGA
            ActivityKind.SOCCER, ActivityKind.BEACH_SOCCER, ActivityKind.FUTSAL -> ExerciseSessionRecord.EXERCISE_TYPE_SOCCER
            ActivityKind.ROWING_MACHINE -> ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE
            ActivityKind.ROWING, ActivityKind.PADDLING, ActivityKind.KAYAKING, ActivityKind.CANOEING, ActivityKind.RAFTING, ActivityKind.DRAGON_BOAT -> ExerciseSessionRecord.EXERCISE_TYPE_ROWING
            ActivityKind.CRICKET -> ExerciseSessionRecord.EXERCISE_TYPE_CRICKET
            ActivityKind.BASKETBALL -> ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL
            ActivityKind.PINGPONG, ActivityKind.TABLE_TENNIS -> ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS
            ActivityKind.BADMINTON, ActivityKind.SHUTTLECOCK -> ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON
            ActivityKind.STRENGTH_TRAINING, ActivityKind.WEIGHTLIFTING, ActivityKind.DUMBBELL, ActivityKind.BARBELL, ActivityKind.DEADLIFT, ActivityKind.PULL_UPS, ActivityKind.PUSH_UPS, ActivityKind.SIT_UPS, ActivityKind.PLANK, ActivityKind.BURPEE, ActivityKind.ABS, ActivityKind.BACK, ActivityKind.UPPER_BODY, ActivityKind.LOWER_BODY, ActivityKind.SMITH_MACHINE, ActivityKind.BATTLE_ROPE -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            ActivityKind.HIKING, ActivityKind.MOUNTAIN_HIKE, ActivityKind.TREKKING -> ExerciseSessionRecord.EXERCISE_TYPE_HIKING
            ActivityKind.CLIMBING, ActivityKind.ROCK_CLIMBING, ActivityKind.CLIMB_INDOOR, ActivityKind.BOULDERING, ActivityKind.FLOOR_CLIMBING, ActivityKind.MOUNTAINEERING -> ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING
            ActivityKind.HANDCYCLING, ActivityKind.HANDCYCLING_INDOOR -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY
            ActivityKind.E_BIKE -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
            ActivityKind.BIKE_COMMUTE -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
            ActivityKind.STAIR_STEPPER, ActivityKind.STAIR_CLIMBER, ActivityKind.STAIRS -> ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE
            ActivityKind.PILATES -> ExerciseSessionRecord.EXERCISE_TYPE_PILATES
            ActivityKind.POOL_SWIM, ActivityKind.ARTISTIC_SWIMMING -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL
            ActivityKind.TENNIS, ActivityKind.PLATFORM_TENNIS, ActivityKind.PICKLEBALL, ActivityKind.PADEL, ActivityKind.SQUASH, ActivityKind.RACQUETBALL -> ExerciseSessionRecord.EXERCISE_TYPE_TENNIS
            ActivityKind.AMERICAN_FOOTBALL, ActivityKind.AUSTRALIAN_FOOTBALL, ActivityKind.RUGBY -> ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN
            ActivityKind.CARDIO, ActivityKind.AEROBICS, ActivityKind.AEROBIC_EXERCISE, ActivityKind.AEROBIC_COMBO, ActivityKind.STEP_AEROBICS -> ExerciseSessionRecord.EXERCISE_TYPE_DANCING
            ActivityKind.BREATHWORK -> ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING
            ActivityKind.MEDITATION, ActivityKind.MIND_AND_BODY -> ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING
            ActivityKind.INDOOR_WALKING -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
            ActivityKind.XC_CLASSIC_SKI, ActivityKind.CROSS_COUNTRY_SKIING -> ExerciseSessionRecord.EXERCISE_TYPE_SKIING
            ActivityKind.SKIING -> ExerciseSessionRecord.EXERCISE_TYPE_SKIING
            ActivityKind.SNOWBOARDING -> ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING
            ActivityKind.GOLF -> ExerciseSessionRecord.EXERCISE_TYPE_GOLF
            ActivityKind.INLINE_SKATING, ActivityKind.ROLLER_SKATING, ActivityKind.SKATING -> ExerciseSessionRecord.EXERCISE_TYPE_SKATING
            ActivityKind.ICE_SKATING, ActivityKind.INDOOR_ICE_SKATING -> ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING
            ActivityKind.SNOWSHOE -> ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING
            ActivityKind.STAND_UP_PADDLEBOARDING -> ExerciseSessionRecord.EXERCISE_TYPE_PADDLING
            ActivityKind.SURFING, ActivityKind.FLOWRIDING -> ExerciseSessionRecord.EXERCISE_TYPE_SURFING
            ActivityKind.WAKEBOARDING, ActivityKind.WAKESURFING -> ExerciseSessionRecord.EXERCISE_TYPE_SURFING
            ActivityKind.WATER_SKIING -> ExerciseSessionRecord.EXERCISE_TYPE_SURFING
            ActivityKind.WINDSURFING -> ExerciseSessionRecord.EXERCISE_TYPE_SURFING
            ActivityKind.KITESURFING -> ExerciseSessionRecord.EXERCISE_TYPE_SURFING
            ActivityKind.BOXING -> ExerciseSessionRecord.EXERCISE_TYPE_BOXING
            ActivityKind.BASEBALL -> ExerciseSessionRecord.EXERCISE_TYPE_BASEBALL
            ActivityKind.SOFTBALL, ActivityKind.SOFTBALL_SLOW_PITCH -> ExerciseSessionRecord.EXERCISE_TYPE_SOFTBALL
            ActivityKind.HIIT -> ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
            ActivityKind.HOCKEY, ActivityKind.ICE_HOCKEY -> ExerciseSessionRecord.EXERCISE_TYPE_ICE_HOCKEY
            ActivityKind.LACROSSE, ActivityKind.LACROSS -> ExerciseSessionRecord.EXERCISE_TYPE_ICE_HOCKEY
            ActivityKind.VOLLEYBALL, ActivityKind.BEACH_VOLLEYBALL -> ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL
            ActivityKind.MIXED_MARTIAL_ARTS, ActivityKind.FREE_SPARRING, ActivityKind.BODY_COMBAT, ActivityKind.CARDIO_COMBAT -> ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS
            ActivityKind.DANCE, ActivityKind.BELLY_DANCE, ActivityKind.JAZZ_DANCE, ActivityKind.LATIN_DANCE, ActivityKind.BALLET, ActivityKind.STREET_DANCE, ActivityKind.ZUMBA, ActivityKind.BALLROOM_DANCE, ActivityKind.BREAKING, ActivityKind.FOLK_DANCE, ActivityKind.HIP_HOP, ActivityKind.MODERN_DANCE, ActivityKind.POLE_DANCE, ActivityKind.SQUARE_DANCE, ActivityKind.PLAZA_DANCING -> ExerciseSessionRecord.EXERCISE_TYPE_DANCING
            ActivityKind.KICKBOXING, ActivityKind.TAE_BO -> ExerciseSessionRecord.EXERCISE_TYPE_BOXING
            ActivityKind.CROSSFIT, ActivityKind.FUNCTIONAL_TRAINING, ActivityKind.PHYSICAL_TRAINING, ActivityKind.FREE_TRAINING, ActivityKind.FITNESS_EXERCISES -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            ActivityKind.TAEKWONDO, ActivityKind.KARATE, ActivityKind.JUDO, ActivityKind.JUJITSU, ActivityKind.KENDO, ActivityKind.MUAY_THAI, ActivityKind.MARTIAL_ARTS -> ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS
            ActivityKind.FENCING -> ExerciseSessionRecord.EXERCISE_TYPE_FENCING
            ActivityKind.CORE_TRAINING -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            ActivityKind.HORIZONTAL_BAR, ActivityKind.PARALLEL_BAR, ActivityKind.PARALLEL_BARS, ActivityKind.MASS_GYMNASTICS -> ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS
            ActivityKind.GYMNASTICS -> ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS
            ActivityKind.TRAMPOLINE -> ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS
            ActivityKind.TAI_CHI -> ExerciseSessionRecord.EXERCISE_TYPE_YOGA
            ActivityKind.HULA_HOOPING, ActivityKind.HULA_HOOP -> ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS
            ActivityKind.ARCHERY -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            ActivityKind.HORSE_RIDING, ActivityKind.EQUESTRIAN -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            ActivityKind.WRESTLING -> ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS
            ActivityKind.HANDBALL -> ExerciseSessionRecord.EXERCISE_TYPE_HANDBALL
            ActivityKind.SAILING, ActivityKind.SAIL_RACE, ActivityKind.SAIL_EXPEDITION -> ExerciseSessionRecord.EXERCISE_TYPE_SAILING
            ActivityKind.SKATEBOARDING -> ExerciseSessionRecord.EXERCISE_TYPE_SKATING
            ActivityKind.PARKOUR -> ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS
            ActivityKind.STRETCHING, ActivityKind.FLEXIBILITY, ActivityKind.ROLLING -> ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING
            ActivityKind.WATER_POLO -> ExerciseSessionRecord.EXERCISE_TYPE_WATER_POLO
            ActivityKind.OBSTACLE_RACE -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            ActivityKind.SLEDDING, ActivityKind.BOBSLEIGH, ActivityKind.LUGE -> ExerciseSessionRecord.EXERCISE_TYPE_SKIING
            ActivityKind.BIATHLON -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            ActivityKind.ORIENTEERING -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            ActivityKind.TRIATHLON, ActivityKind.MULTISPORT -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            ActivityKind.DIVING, ActivityKind.FREE_DIVING, ActivityKind.APNEA_TRAINING, ActivityKind.APNEA_TEST, ActivityKind.SCUBA_DIVING, ActivityKind.SNORKELING -> ExerciseSessionRecord.EXERCISE_TYPE_SCUBA_DIVING
            ActivityKind.PARAGLIDING, ActivityKind.HANG_GLIDING, ActivityKind.JUMPMASTER, ActivityKind.PARACHUTING, ActivityKind.SKY_DIVING -> ExerciseSessionRecord.EXERCISE_TYPE_PARAGLIDING
            ActivityKind.TRACK_AND_FIELD, ActivityKind.ATHLETICS, ActivityKind.JAVELIN, ActivityKind.LONG_JUMP, ActivityKind.HIGH_JUMP, ActivityKind.SHOT -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            ActivityKind.DISC_GOLF, ActivityKind.FRISBEE, ActivityKind.ULTIMATE_DISC -> ExerciseSessionRecord.EXERCISE_TYPE_FRISBEE_DISC
            ActivityKind.WATER_TUBING, ActivityKind.JET_SKIING, ActivityKind.WATER_SCOOTER -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            ActivityKind.BMX -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
            ActivityKind.KABADDI -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            ActivityKind.CURLING -> ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING
            ActivityKind.COOLDOWN -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            ActivityKind.PUSH_WALK_SPEED, ActivityKind.INDOOR_PUSH_WALK_SPEED -> ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR
            ActivityKind.PUSH_RUN_SPEED, ActivityKind.INDOOR_PUSH_RUN_SPEED -> ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR
            ActivityKind.FITNESS_GAMING -> ExerciseSessionRecord.EXERCISE_TYPE_DANCING

            ActivityKind.ACTIVITY, ActivityKind.EXERCISE, ActivityKind.TRAINING,
            ActivityKind.FITNESS_EQUIPMENT, ActivityKind.INDOOR_FITNESS, ActivityKind.SOMATOSENSORY_GAME, ActivityKind.VIDEO_GAMING,
            ActivityKind.HEALTH_SNAPSHOT, ActivityKind.TACTICAL, ActivityKind.GRINDING, ActivityKind.WINTER_SPORT, ActivityKind.TEAM_SPORT,
            ActivityKind.SNOW_SPORTS, ActivityKind.DISC_SPORTS, ActivityKind.OTHER_WATER_SPORTS, ActivityKind.OTHER_WINTER_SPORTS,
            ActivityKind.MARINE, ActivityKind.PARA_SPORT, ActivityKind.RACKET, ActivityKind.NAVIGATE, ActivityKind.INDOOR_TRACK,
            ActivityKind.TRANSITION, ActivityKind.VIVOMOVE_HR_TRANSITION,
            ActivityKind.FLYING, ActivityKind.MOTORCYCLING, ActivityKind.BOATING, ActivityKind.DRIVING, ActivityKind.HUNTING, ActivityKind.FISHING,
            ActivityKind.AUTO_RACING, ActivityKind.SNOWMOBILING, ActivityKind.KARTING, ActivityKind.BILLIARDS, ActivityKind.BOWLING,
            ActivityKind.DARTS, ActivityKind.KITE_FLYING, ActivityKind.SWING, ActivityKind.GATEBALL, ActivityKind.SEPAK_TAKRAW,
            ActivityKind.BOARD_GAME, ActivityKind.BRIDGE, ActivityKind.CHECKERS, ActivityKind.CHESS, ActivityKind.ESPORTS,
            ActivityKind.HACKY_SACK, ActivityKind.JAI_ALAI, ActivityKind.SHUFFLEBOARD, ActivityKind.TABLE_FOOTBALL,
            ActivityKind.TUG_OF_WAR, ActivityKind.WALL_BALL, ActivityKind.WEIQI, ActivityKind.LASER_TAG, ActivityKind.BILLIARD_POOL,
            ActivityKind.ATV, ActivityKind.POWERBOATING, ActivityKind.SHOOTING, ActivityKind.BOCCE,
            ActivityKind.UNKNOWN, ActivityKind.NOT_MEASURED, ActivityKind.NOT_WORN -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT

            ActivityKind.LIGHT_SLEEP, ActivityKind.DEEP_SLEEP, ActivityKind.REM_SLEEP, ActivityKind.AWAKE_SLEEP, ActivityKind.SLEEP_ANY -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT

            else -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
        }
    }
}

