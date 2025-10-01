/*  Copyright (C) 2025 Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionLocationSymbol extends FieldDefinition {

    public FieldDefinitionLocationSymbol(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
        super(localNumber, size, baseType, name, scale, offset);
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj != null) {
            final Number raw = (Number) rawObj;
            return LocationSymbol.fromId(raw.intValue());
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof LocationSymbol locationSymbol) {
            baseType.encode(byteBuffer, locationSymbol.getId(), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }

    public enum LocationSymbol {
        Airport(0, "Airport"),
        Amusement_Park(1, "Amusement Park"),
        Anchor(2, "Anchor"),
        Ball_Park(3, "Ball Park"),
        Bank(4, "Bank"),
        Bar(5, "Bar"),
        Block_Blue(6, "Block, Blue"),
        Boat_Ramp(7, "Boat Ramp"),
        Bowling(8, "Bowling"),
        Bridge(9, "Bridge"),
        Building(10, "Building"),
        Campground(11, "Campground"),
        Car(12, "Car"),
        Car_Rental(13, "Car Rental"),
        Car_Repair(14, "Car Repair"),
        Cemetery(15, "Cemetery"),
        Church(16, "Church"),
        City_Large(17, "City (Large)"),
        City_Medium(18, "City (Medium)"),
        City_Small(19, "City (Small)"),
        Civil(20, "Civil"),
        Controlled_Area(21, "Controlled Area"),
        Convenience_Store(22, "Convenience Store"),
        Crossing(23, "Crossing"),
        Dam(24, "Dam"),
        Skull_and_Crossbones(25, "Skull and Crossbones"),
        Danger_Area(26, "Danger Area"),
        Department_Store(27, "Department Store"),
        Diver_Down_Flag_1(28, "Diver Down Flag 1"),
        Diver_Down_Flag_2(29, "Diver Down Flag 2"),
        Drinking_Water(30, "Drinking Water"),
        Unknown_31(31, "Unknown 31"),
        Fast_Food(32, "Fast Food"),
        Fishing_Area(33, "Fishing Area"),
        Fitness_Center(34, "Fitness Center"),
        Forest(35, "Forest"),
        Gas_Station(36, "Gas Station"),
        Glider_Area(37, "Glider Area"),
        Golf_Course(38, "Golf Course"),
        Lodging(39, "Lodging"),
        Hunting_Area(40, "Hunting Area"),
        Information(41, "Information"),
        Live_Theater(42, "Live Theater"),
        Light(43, "Light"),
        Man_Overboard(44, "Man Overboard"),
        Medical_Facility(45, "Medical Facility"),
        Mine(46, "Mine"),
        Movie_Theater(47, "Movie Theater"),
        Museum(48, "Museum"),
        Oil_Field(49, "Oil Field"),
        Parachute_Area(50, "Parachute Area"),
        Park(51, "Park"),
        Parking_Area(52, "Parking Area"),
        Pharmacy(53, "Pharmacy"),
        Picnic_Area(54, "Picnic Area"),
        Pizza(55, "Pizza"),
        Post_Office(56, "Post Office"),
        RV_Park(57, "RV Park"),
        Residence(58, "Residence"),
        Restricted_Area(59, "Restricted Area"),
        Restaurant(60, "Restaurant"),
        Restroom(61, "Restroom"),
        Scales(62, "Scales"),
        Scenic_Area(63, "Scenic Area"),
        School(64, "School"),
        Shipwreck(65, "Shipwreck"),
        Shopping_Center(66, "Shopping Center"),
        Short_Tower(67, "Short Tower"),
        Shower(68, "Shower"),
        Skiing_Area(69, "Skiing Area"),
        Stadium(70, "Stadium"),
        Summit(71, "Summit"),
        Swimming_Area(72, "Swimming Area"),
        Tall_Tower(73, "Tall Tower"),
        Telephone(74, "Telephone"),
        Toll_Booth(75, "Toll Booth"),
        Trail_Head(76, "Trail Head"),
        Truck_Stop(77, "Truck Stop"),
        Tunnel(78, "Tunnel"),
        Ultralight_Area(79, "Ultralight Area"),
        Zoo(80, "Zoo"),
        Geocache(81, "Geocache"),
        Geocache_Found(82, "Geocache Found"),
        Flag_Blue(83, "Flag, Blue"),
        Pin_Blue(84, "Pin, Blue"),
        Bike_Trail(85, "Bike Trail"),
        Ice_Skating(86, "Ice Skating"),
        Unknown_87(87, "Unknown 87"),
        Beacon(88, "Beacon"),
        Horn(89, "Horn"),
        Beach(90, "Beach"),
        Buoy_White(91, "Buoy, White"),
        Wrecker(92, "Wrecker"),
        Navaid_Amber(93, "Navaid, Amber"),
        Navaid_Black(94, "Navaid, Black"),
        Navaid_Blue(95, "Navaid, Blue"),
        Navaid_GreenWhite(96, "Navaid, Green/White"),
        Navaid_Green(97, "Navaid, Green"),
        Navaid_GreenRed(98, "Navaid, Green/Red"),
        Navaid_Orange(99, "Navaid, Orange"),
        Navaid_RedGreen(100, "Navaid, Red/Green"),
        Navaid_RedWhite(101, "Navaid, Red/White"),
        Navaid_Red(102, "Navaid, Red"),
        Navaid_Violet(103, "Navaid, Violet"),
        Navaid_White(104, "Navaid, White"),
        Navaid_WhiteGreen(105, "Navaid, White/Green"),
        Navaid_WhiteRed(106, "Navaid, White/Red"),
        Unknown_107(107, "Unknown 107"),
        Bell(108, "Bell"),
        Block_Green(109, "Block, Green"),
        Block_Red(110, "Block, Red"),
        Food_Source(111, "Food Source"),
        Unknown_112(112, "Unknown 112"),
        Unknown_113(113, "Unknown 113"),
        Unknown_114(114, "Unknown 114"),
        Unknown_115(115, "Unknown 115"),
        Flag_Green(116, "Flag, Green"),
        Flag_Red(117, "Flag, Red"),
        Pin_Green(118, "Pin, Green"),
        Pin_Red(119, "Pin, Red"),
        ATV(120, "ATV"),
        Big_Game(121, "Big Game"),
        Blind(122, "Blind"),
        Blood_Trail(123, "Blood Trail"),
        Cover(124, "Cover"),
        Covey(125, "Covey"),
        Unknown_126(126, "Unknown 126"),
        Furbearer(127, "Furbearer"),
        Lodge(128, "Lodge"),
        Small_Game(129, "Small Game"),
        Animal_Tracks(130, "Animal Tracks"),
        Treed_Quarry(131, "Treed Quarry"),
        Tree_Stand(132, "Tree Stand"),
        Truck(133, "Truck"),
        Upland_Game(134, "Upland Game"),
        Waterfowl(135, "Waterfowl"),
        Water_Source(136, "Water Source"),
        ;

        private final int id;
        private final String gxpSymbol;

        LocationSymbol(int i, String symbol) {
            id = i;
            gxpSymbol = symbol;
        }

        @Nullable
        public static LocationSymbol fromId(int id) {
            for (LocationSymbol symbol : values()) {
                if (id == symbol.id) {
                    return symbol;
                }
            }
            return null;
        }

        @Nullable
        public static LocationSymbol fromGxpSymbol(String sym) {
            if (sym == null || sym.length() == 0) {
                return null;
            }
            for (LocationSymbol symbol : values()) {
                if (symbol.gxpSymbol.equals(sym)) {
                    return symbol;
                }
            }
            // Garmin Basecamp uses the blue flag for unknown symbols
            return Flag_Blue;
        }

        public int getId() {
            return id;
        }

        public String getGxpSymbol() {
            return gxpSymbol;
        }

        @Override
        public String toString() {
            return gxpSymbol;
        }
    }
}
