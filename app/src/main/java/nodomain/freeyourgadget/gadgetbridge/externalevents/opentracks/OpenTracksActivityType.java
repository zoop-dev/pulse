package nodomain.freeyourgadget.gadgetbridge.externalevents.opentracks;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

/**
 * As per <a href="https://codeberg.org/OpenTracksApp/OpenTracks/src/branch/main/src/main/java/de/dennisguse/opentracks/data/models/ActivityType.java">ActivityType.java</a>
 */
public enum OpenTracksActivityType {
    AIRPLANE("airplane"),
    ATV("ATV"),
    BIKING("biking"),
    BLIMP("blimp"),
    BOAT("boat"),
    CLIMBING("climbing"),
    COMMERCIAL_AIRPLANE("commercial airplane"),
    CROSS_COUNTRY_SKIING("cross-country skiing"),
    CYCLING("cycling"),
    DIRT_BIKE("dirt bike"),
    DONKEY_BACK_RIDING("donkey back riding"),
    DRIVING("driving"),
    DRIVING_BUS("driving bus"),
    DRIVING_CAR("driving car"),
    ESCOOTER("escooter"),
    FERRY("ferry"),
    FRISBEE("frisbee"),
    GLIDING("gliding"),
    HANG_GLIDING("hang gliding"),
    HELICOPTER("helicopter"),
    HIKING("hiking"),
    HORSE_BACK_RIDING("horse back riding"),
    HOT_AIR_BALLOON("hot air balloon"),
    ICE_SAILING("ice sailing"),
    INLINE_SKATING("inline skating"),
    KAYAKING("kayaking"),
    KITE_SURFING("kite surfing"),
    LAND_SAILING("land sailing"),
    MIXED_TYPE("mixed type"),
    MOTOR_BIKE("motor bike"),
    MOTOR_BOATING("motor boating"),
    MOUNTAIN_BIKING("mountain biking"),
    OFF_TRAIL_HIKING("off trail hiking"),
    OTHER("other"),
    PADDLING("paddling"),
    PARA_GLIDING("para gliding"),
    RC_AIRPLANE("RC airplane"),
    RC_BOAT("RC boat"),
    RC_HELICOPTER("RC helicopter"),
    RIDING("riding"),
    ROAD_BIKING("road biking"),
    ROLLER_SKIING("roller skiing"),
    ROWING("rowing"),
    RUNNING("running"),
    SAILING("sailing"),
    KICKSCOOTER("kickscooter"),
    SEAPLANE("seaplane"),
    SKATE_BOARDING("skateboarding"),
    SKATING("skating"),
    SKIING("skiing"),
    SKY_JUMPING("sky jumping"),
    SLED("sled"),
    SNOW_BOARDING("snowboarding"),
    SNOW_SHOEING("snow shoeing"),
    SPEED_WALKING("speed walking"),
    STREET_RUNNING("street running"),
    SURFING("surfing"),
    TRACK_CYCLING("track cycling"),
    TRACK_RUNNING("track running"),
    TRAIL_HIKING("trail hiking"),
    TRAIL_RUNNING("trail running"),
    TRAIN("train"),
    ULTIMATE_FRISBEE("ultimate frisbee"),
    WAKEBOARDING("wakeboarding"),
    WALKING("walking"),
    WATER_SKIING("water skiing"),
    WIND_SURFING("wind surfing"),
    SWIMMING("swimming"),
    SWIMMING_OPEN("swimming in open water"),
    WORKOUT("workout"),
    UNKNOWN("unknown"),
    ;

    private final String id;

    OpenTracksActivityType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static OpenTracksActivityType fromActivityKind(final ActivityKind activityKind) {
        return switch (activityKind) {
            case ATV -> ATV;
            case CYCLING, INDOOR_CYCLING, OUTDOOR_CYCLING, HANDCYCLING -> BIKING;
            case BOATING, DRAGON_BOAT, POWERBOATING -> BOAT;
            case CLIMBING -> CLIMBING;
            case CROSS_COUNTRY_SKIING -> CROSS_COUNTRY_SKIING;
            case DRIVING -> DRIVING;
            case FRISBEE -> FRISBEE;
            case HANG_GLIDING -> HANG_GLIDING;
            case HIKING -> HIKING;
            case HORSE_RIDING -> HORSE_BACK_RIDING;
            case ICE_SKATING -> ICE_SAILING;
            case INLINE_SKATING -> INLINE_SKATING;
            case KAYAKING -> KAYAKING;
            case KITESURFING -> KITE_SURFING;
            case MOTORCYCLING -> MOTOR_BIKE;
            case MOUNTAIN_BIKE -> MOUNTAIN_BIKING;
            case PADDLING -> PADDLING;
            case PARAGLIDING -> PARA_GLIDING;
            case ROAD_BIKE -> ROAD_BIKING;
            case ROWING -> ROWING;
            case RUNNING -> RUNNING;
            case SAILING -> SAILING;
            case SKATEBOARDING -> SKATE_BOARDING;
            case SKATING -> SKATING;
            case SKIING -> SKIING;
            case SKY_DIVING -> SKY_JUMPING;
            case SLEDDING -> SLED;
            case SNOWBOARDING -> SNOW_BOARDING;
            case SNOWSHOE -> SNOW_SHOEING;
            case SURFING -> SURFING;
            case TRACK_RUN -> TRACK_RUNNING;
            case TRAIL_RUN -> TRAIL_RUNNING;
            case TRAINING -> TRAIN;
            case ULTIMATE_DISC -> ULTIMATE_FRISBEE;
            case WAKEBOARDING -> WAKEBOARDING;
            case WALKING -> WALKING;
            case WATER_SKIING -> WATER_SKIING;
            case WINDSURFING -> WIND_SURFING;
            case SWIMMING -> SWIMMING;
            case SWIMMING_OPENWATER -> SWIMMING_OPEN;
            default -> UNKNOWN;
        };
    }
}
