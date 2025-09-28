/*  Copyright (C) 2015-2024 115ek, akasaka / Genjitsu Labs, Alicia Hormann,
    Andreas Böhler, Andreas Shimokawa, Andrew Watkins, angelpup, Carsten Pfeiffer,
    Cre3per, Damien Gaignon, DanialHanif, Daniel Dakhno, Daniele Gobbetti, Daniel
    Thompson, Da Pa, Dmytro Bielik, Frank Ertl, Gabriele Monaco, GeekosaurusR3x,
    Guido Jäkel, Gordon Williams, Jean-François Greffier, jfgreffier, jhey, João Paulo
    Barraca, Jochen S, Johannes Krude, José Rebelo, ksiwczynski, ladbsoft,
    Lesur Frederic, Maciej Kuśnierz, mamucho, Manuel Ruß, Maxime Reyrolle,
    maxirnilian, Michael, narektor, Noodlez, odavo32nof, opavlov, pangwalla,
    Pavel Elagin, Petr Kadlec, Petr Vaněk, protomors, Quallenauge, Quang Ngô,
    Raghd Hamzeh, Sami Alaoui, Sebastian Kranz, sedy89, Sophanimus, Stefan Bora,
    Taavi Eomäe, thermatk, tiparega, Vadim Kaushan, x29a, xaos, Yoran Vulker,
    Yukai Li

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

import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.UnknownDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.aawireless.AAWirelessCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.asteroidos.AsteroidOSDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.atcbleoepl.ATCBLEOEPLCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.bandwpseries.BandWPSeriesDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.banglejs.BangleJSCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.binary_sensor.coordinator.BinarySensorCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.ecbs100.CasioECBS100DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.gb6900.CasioGB6900DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.gbx100.CasioGBX100DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.gwb5600.CasioGMWB5000DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.gwb5600.CasioGWB5600DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.CmfWatchPro2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.CmfWatchProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.coospo.CoospoH6Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.coospo.CoospoHW807Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.coospo.CoospoHW9Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.cycling_sensor.coordinator.CyclingSensorCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.divoom.PixooCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.domyos.DomyosT540Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.earfun.EarFunAirPro4Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.earfun.EarFunAirSCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.evenrealities.G1DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.femometer.FemometerVinca2DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.fitpro.colacao.ColaCao21Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.fitpro.colacao.ColaCao23Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.flipper.zero.FlipperZeroCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.galaxy_buds.GalaxyBuds2DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.galaxy_buds.GalaxyBuds2ProDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.galaxy_buds.GalaxyBudsDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.galaxy_buds.GalaxyBudsLiveDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.galaxy_buds.GalaxyBudsProDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.bike.GarminEdge130PlusCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.bike.GarminEdge540Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.bike.GarminEdgeExplore2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.hrm.GarminHrmProPlusCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.gps.GarminETrexSeCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.gps.GarminInReachMini2;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.hrm.GarminHrm200Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.descent.GarminDescentMk3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.enduro.GarminEnduro3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.epix.GarminEpixProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.fenix.GarminFenix3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.fenix.GarminFenix3HrCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.fenix.GarminFenix5Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.fenix.GarminFenix5PlusCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.fenix.GarminFenix5XPlusCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.fenix.GarminFenix6Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.fenix.GarminFenix6SProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.fenix.GarminFenix6SSapphireCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.fenix.GarminFenix6SapphireCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.fenix.GarminFenix6XProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.fenix.GarminFenix6XProSolarCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.fenix.GarminFenix7Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.fenix.GarminFenix7ProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.fenix.GarminFenix7SCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.fenix.GarminFenix7SProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.fenix.GarminFenix7XCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.fenix.GarminFenix7XProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.fenix.GarminFenix8Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner.GarminForerunner165Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner.GarminForerunner165MusicCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner.GarminForerunner235Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner.GarminForerunner245Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner.GarminForerunner245MusicCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner.GarminForerunner255Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner.GarminForerunner255MusicCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner.GarminForerunner255SCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner.GarminForerunner255SMusicCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner.GarminForerunner265Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner.GarminForerunner265SCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner.GarminForerunner45Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner.GarminForerunner55Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner.GarminForerunner620Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner.GarminForerunner630Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner.GarminForerunner645MusicCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner.GarminForerunner735XTCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner.GarminForerunner945Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner.GarminForerunner955Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner.GarminForerunner965Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct.GarminInstinct2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct.GarminInstinct2SCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct.GarminInstinct2SSolarCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct.GarminInstinct2SolTacCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct.GarminInstinct2SolarCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct.GarminInstinct2XSolarCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct.GarminInstinct2XTacticalCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct.GarminInstinct3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct.GarminInstinct3TacticalCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct.GarminInstinctCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct.GarminInstinctCrossoverCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct.GarminInstinctECoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct.GarminInstinctSolarCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct.GarminInstinctTacticalCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.lily.GarminLily2ActiveCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.swim.GarminSwim2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.venu.GarminVenu2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.venu.GarminVenu2PlusCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.venu.GarminVenu2SCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.venu.GarminVenu3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.venu.GarminVenu3SCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.venu.GarminVenuCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.venu.GarminVenuSq2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.venu.GarminVenuSqCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.venu.GarminVenuX1Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivoactive.GarminVivoActive3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivoactive.GarminVivoActive4Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivoactive.GarminVivoActive4SCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivoactive.GarminVivoActive5Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivoactive.GarminVivoActive6Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivoactive.GarminVivoActiveHrCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivomove.GarminVivomoveHrCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivomove.GarminVivomoveSportCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivomove.GarminVivomoveStyleCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivomove.GarminVivomoveTrendCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivosmart.GarminVivosmart3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivosmart.GarminVivosmart4Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivosmart.GarminVivosmart5Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivosport.GarminVivosportCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.generic_headphones.GenericHeadphonesCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.generic_hr.GenericHeartRateCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.generic_scale.GenericWeightScaleCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.gloryfit.watches.DotnP66DCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.gloryfit.watches.HaylouWatch2ProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.gloryfit.watches.OukitelBT103Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.gloryfit.watches.Y66Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.gree.GreeAcCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.hama.fit6900.HamaFit6900DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.hplus.EXRIZUK8Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.hplus.HPlusCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.hplus.MakibesF68Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.hplus.Q8Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.hplus.SG2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitband5.AmazfitBand5Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbip.AmazfitBipCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbip.AmazfitBipLiteCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbip3.AmazfitBip3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbip3pro.AmazfitBip3ProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbips.AmazfitBipSCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbips.AmazfitBipSLiteCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbipu.AmazfitBipUCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbipupro.AmazfitBipUProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitcor.AmazfitCorCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitcor2.AmazfitCor2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitgtr.AmazfitGTRCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitgtr.AmazfitGTRLiteCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitgtr2.AmazfitGTR2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitgtr2.AmazfitGTR2eCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitgts.AmazfitGTSCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitgts2.AmazfitGTS2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitgts2.AmazfitGTS2MiniCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitgts2.AmazfitGTS2eCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitneo.AmazfitNeoCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitpop.AmazfitPopCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitpoppro.AmazfitPopProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfittrex.AmazfitTRexCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfittrexpro.AmazfitTRexProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitvergel.AmazfitVergeLCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitx.AmazfitXCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband2.MiBand2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband2.MiBand2HRXCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband3.MiBand3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband4.MiBand4Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband5.MiBand5Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband6.MiBand6Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppe.ZeppECoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.rings.AmazfitHelioRingCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.straps.AmazfitHelioStrapCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitActive2RoundCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitActive2NfcCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitActive2SquareCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitActiveCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitActiveEdgeCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitBalance2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitBalanceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitBand7Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitBip5Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitBip5UnityCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitBip6Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitCheetahProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitCheetahRoundCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitCheetahSquareCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitFalconCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitGTR3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitGTR3ProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitGTR4Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitGTRMiniCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitGTS3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitGTS4Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitGTS4MiniCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitTRex2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitTRex3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.AmazfitTRexUltraCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches.MiBand7Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.freearc.HuaweiFreearcCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.freebuds5i.HuaweiFreebuds5iCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.freebuds6.HuaweiFreebuds6Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.freebudspro.HuaweiFreebudsProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.honorband3.HonorBand3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.honorband4.HonorBand4Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.honorband5.HonorBand5Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.honorband6.HonorBand6Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.honorband7.HonorBand7Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.honormagicwatch2.HonorMagicWatch2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.honorwatch4.HonorWatch4Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.honorwatchgs3.HonorWatchGS3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.honorwatchgspro.HonorWatchGSProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiband10.HuaweiBand10Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiband2pro.HuaweiBand2ProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiband3pro.HuaweiBand3ProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiband4pro.HuaweiBand4ProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiband6.HuaweiBand6Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiband7.HuaweiBand7Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiband8.HuaweiBand8Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiband9.HuaweiBand9Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweibandaw70.HuaweiBandAw70Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweitalkbandb6.HuaweiTalkBandB6Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiwatch3.HuaweiWatch3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiwatch4pro.HuaweiWatch4ProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiwatch5.HuaweiWatch5Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiwatchd2.HuaweiWatchD2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiwatchfit.HuaweiWatchFitCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiwatchfit2.HuaweiWatchFit2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiwatchfit3.HuaweiWatchFit3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiwatchfit4pro.HuaweiWatchFit4ProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiwatchgt.HuaweiWatchGTCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiwatchgt2.HuaweiWatchGT2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiwatchgt2e.HuaweiWatchGT2eCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiwatchgt3.HuaweiWatchGT3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiwatchgt4.HuaweiWatchGT4Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiwatchgt5.HuaweiWatchGT5Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiwatchgt6.HuaweiWatchGT6Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiwatchgtcyber.HuaweiWatchGTCyberCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiwatchgtrunner.HuaweiWatchGTRunnerCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiwatchultimate.HuaweiWatchUltimateCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.id115.ID115Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.idasen.IdasenCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.itag.ITagCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.jyou.BFH16DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.jyou.TeclastH30.TeclastH30Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.jyou.y5.Y5Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.BohemicSmartBraceletDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.VivitarHrBpMonitorActivityTrackerCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.lenovo.watchxplus.WatchXPlusDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.makibeshr3.MakibesHR3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.marstek.MarstekB2500DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.mijia_lywsd.MijiaLywsd02Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.mijia_lywsd.MijiaLywsd03Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.mijia_lywsd.MijiaMhoC303Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.mijia_lywsd.MijiaXmwsdj04Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.miscale.MiCompositionScaleCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.miscale.MiSmartScaleCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.moondrop.MoondropSpaceTravelCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.AdvanWatchSE1AICoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.BlackviewR60Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.C20Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.ColmiI28UltraCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.ColmiP76Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.ColmiP80Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.ColmiV72Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.ColmiV76Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.ColmiV89Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.FireBolttTalkCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.HC96Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.KT80Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.KsixVentureCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.MisirunC17Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.R50ProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.RainbuvvyT97Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.ViranC29Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.no1f1.No1F1Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.nothing.CmfBudsPro2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.nothing.Ear1Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.nothing.Ear2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.nothing.EarACoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.nothing.EarStickCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.nut.NutCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.onemoresonoflow.OneMoreSonoFlowCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.oppo.OppoEncoAir2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.oppo.OppoEncoAirCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.oppo.OppoEncoBuds2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.pinetime.PineTimeJFCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.pixel.PixelBudsACoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.polar.PolarH10DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.polar.PolarH9DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.qc35.QC35Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.QHybridCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.realme.RealmeBudsAir5ProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.realme.RealmeBudsT100Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.realme.RealmeBudsT110Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.realme.RealmeBudsT300Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.redmibuds.RedmiBuds3ProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.redmibuds.RedmiBuds4ActiveCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.redmibuds.RedmiBuds5ProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.redmibuds.RedmiBuds6ActiveCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.redmibuds.RedmiBuds6ProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.roidmi.Roidmi1Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.roidmi.Roidmi3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.scannable.ScannableDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.shokz.ShokzOpenSwimProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.smaq2oss.SMAQ2OSSCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.soflow.SoFlowCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators.SonyLinkBudsCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators.SonyLinkBudsSCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators.SonyWF1000XM3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators.SonyWF1000XM4Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators.SonyWF1000XM5Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators.SonyWFC500Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators.SonyWFC510Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators.SonyWFC700NCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators.SonyWFC710NCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators.SonyWFSP800NCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators.SonyWH1000XM2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators.SonyWH1000XM3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators.SonyWH1000XM4Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators.SonyWH1000XM5Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators.SonyWHULT900NCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators.SonyWIC100Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators.SonyWISP600NCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3.SonyWena3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sonyswr12.SonySWR12DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.soundcore.aerofit.SoundcoreAeroFit2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.soundcore.liberty3_pro.SoundcoreLiberty3ProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.soundcore.liberty4_nc.SoundcoreLiberty4NCCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.soundcore.motion300.SoundcoreMotion300Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.soundcore.q30.SoundcoreQ30Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.supercars.SuperCarsCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.test.TestDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.thermalprinter.ThermalPrinterCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.tlw64.TLW64Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.um25.Coordinator.UM25Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.vesc.VescCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.vibratissimo.VibratissimoCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.waspos.WaspOSCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.watch9.Watch9DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.withingssteelhr.WithingsSteelHRDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.MiBand10Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.MiBand7ProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.MiBand8ActiveCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.MiBand8Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.MiBand8ProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.MiBand9ActiveCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.MiBand9Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.MiBand9ProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.MiWatchColorSportCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.MiWatchLiteCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.RedmiSmartBand2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.RedmiSmartBand3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.RedmiSmartBandProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.RedmiWatch2Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.RedmiWatch2LiteCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.RedmiWatch3ActiveCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.RedmiWatch3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.RedmiWatch4Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.RedmiWatch5ActiveCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.RedmiWatch5Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.RedmiWatch5LiteCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.XiaomiWatchS1ActiveCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.XiaomiWatchS1Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.XiaomiWatchS1ProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.XiaomiWatchS3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches.XiaomiWatchS4Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xwatch.XWatchCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.yawell.ring.ColmiR02Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.yawell.ring.ColmiR03Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.yawell.ring.ColmiR06Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.yawell.ring.ColmiR07Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.yawell.ring.ColmiR09Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.yawell.ring.ColmiR10Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.yawell.ring.ColmiR12Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.yawell.ring.H59Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.yawell.ring.YawellR10Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.yawell.ring.YawellR11Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.zetime.ZeTimeCoordinator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.gatt_client.BleGattClientCoordinator;

/**
 * For every supported device, a device type constant must exist.
 * <p>
 * Note: they name of the enum is stored in the DB, so it is fixed forever,
 * and may not be changed.
 * <p>
 * Migration note: As of <a href="https://codeberg.org/Freeyourgadget/Gadgetbridge/pulls/3347">#3347</a>,
 * the numeric device id is not used anymore. If your database has development devices that still used
 * the numeric ID, you need to update assets/migrations/devicetype.json before installing Gadgetbridge
 * after rebasing, in order for your device to be migrated correctly. If you failed to do this and the
 * device is now not being displayed, please update the file and uncomment the call to migrateDeviceTypes
 * in GBApplication.
 */
public enum DeviceType {
    UNKNOWN(UnknownDeviceCoordinator.class),
    PEBBLE(PebbleCoordinator.class),
    MIBAND(MiBandCoordinator.class),
    MIBAND2(MiBand2Coordinator.class),
    MIBAND2_HRX(MiBand2HRXCoordinator.class),
    AMAZFITBIP(AmazfitBipCoordinator.class),
    AMAZFITCOR(AmazfitCorCoordinator.class),
    MIBAND3(MiBand3Coordinator.class),
    AMAZFITCOR2(AmazfitCor2Coordinator.class),
    MIBAND4(MiBand4Coordinator.class),
    AMAZFITBIP_LITE(AmazfitBipLiteCoordinator.class),
    AMAZFITGTR(AmazfitGTRCoordinator.class),
    AMAZFITGTS(AmazfitGTSCoordinator.class),
    AMAZFITBIPS(AmazfitBipSCoordinator.class),
    AMAZFITGTR_LITE(AmazfitGTRLiteCoordinator.class),
    AMAZFITTREX(AmazfitTRexCoordinator.class),
    MIBAND5(MiBand5Coordinator.class),
    AMAZFITBAND5(AmazfitBand5Coordinator.class),
    AMAZFITBIPS_LITE(AmazfitBipSLiteCoordinator.class),
    AMAZFITGTR2(AmazfitGTR2Coordinator.class),
    AMAZFITGTS2(AmazfitGTS2Coordinator.class),
    AMAZFITBIPU(AmazfitBipUCoordinator.class),
    AMAZFITVERGEL(AmazfitVergeLCoordinator.class),
    AMAZFITBIPUPRO(AmazfitBipUProCoordinator.class),
    AMAZFITNEO(AmazfitNeoCoordinator.class),
    AMAZFITGTS2_MINI(AmazfitGTS2MiniCoordinator.class),
    ZEPP_E(ZeppECoordinator.class),
    AMAZFITGTR2E(AmazfitGTR2eCoordinator.class),
    AMAZFITGTS2E(AmazfitGTS2eCoordinator.class),
    AMAZFITX(AmazfitXCoordinator.class),
    MIBAND6(MiBand6Coordinator.class),
    AMAZFITTREXPRO(AmazfitTRexProCoordinator.class),
    AMAZFITPOP(AmazfitPopCoordinator.class),
    AMAZFITPOPPRO(AmazfitPopProCoordinator.class),
    MIBAND7(MiBand7Coordinator.class),
    MIBAND7PRO(MiBand7ProCoordinator.class),
    MIBAND8(MiBand8Coordinator.class),
    MIBAND8ACTIVE(MiBand8ActiveCoordinator.class),
    MIBAND8PRO(MiBand8ProCoordinator.class),
    MIBAND9(MiBand9Coordinator.class),
    MIBAND9ACTIVE(MiBand9ActiveCoordinator.class),
    MIBAND9PRO(MiBand9ProCoordinator.class),
    MIBAND10(MiBand10Coordinator.class),
    MIWATCHLITE(MiWatchLiteCoordinator.class),
    MIWATCHCOLORSPORT(MiWatchColorSportCoordinator.class),
    REDMIBUDS3PRO(RedmiBuds3ProCoordinator.class),
    REDMIBUDS4ACTIVE(RedmiBuds4ActiveCoordinator.class),
    REDMIBUDS5PRO(RedmiBuds5ProCoordinator.class),
    REDMIBUDS6ACTIVE(RedmiBuds6ActiveCoordinator.class),
    REDMIBUDS6PRO(RedmiBuds6ProCoordinator.class),
    REDMIWATCH3ACTIVE(RedmiWatch3ActiveCoordinator.class),
    REDMIWATCH3(RedmiWatch3Coordinator.class),
    REDMISMARTBAND2(RedmiSmartBand2Coordinator.class),
    REDMISMARTBAND3(RedmiSmartBand3Coordinator.class),
    REDMIWATCH2(RedmiWatch2Coordinator.class),
    REDMIWATCH2LITE(RedmiWatch2LiteCoordinator.class),
    REDMISMARTBANDPRO(RedmiSmartBandProCoordinator.class),
    REDMIWATCH4(RedmiWatch4Coordinator.class),
    REDMIWATCH5(RedmiWatch5Coordinator.class),
    REDMIWATCH5ACTIVE(RedmiWatch5ActiveCoordinator.class),
    REDMIWATCH5LITE(RedmiWatch5LiteCoordinator.class),
    XIAOMI_WATCH_S1_ACTIVE(XiaomiWatchS1ActiveCoordinator.class),
    XIAOMI_WATCH_S1_PRO(XiaomiWatchS1ProCoordinator.class),
    XIAOMI_WATCH_S1(XiaomiWatchS1Coordinator.class),
    XIAOMI_WATCH_S3(XiaomiWatchS3Coordinator.class),
    XIAOMI_WATCH_S4(XiaomiWatchS4Coordinator.class),
    AMAZFITGTS3(AmazfitGTS3Coordinator.class),
    AMAZFITGTR3(AmazfitGTR3Coordinator.class),
    AMAZFITGTR4(AmazfitGTR4Coordinator.class),
    AMAZFITBAND7(AmazfitBand7Coordinator.class),
    AMAZFITGTS4(AmazfitGTS4Coordinator.class),
    AMAZFITGTS4MINI(AmazfitGTS4MiniCoordinator.class),
    AMAZFITTREX2(AmazfitTRex2Coordinator.class),
    AMAZFITTREX3(AmazfitTRex3Coordinator.class),
    AMAZFITGTR3PRO(AmazfitGTR3ProCoordinator.class),
    AMAZFITBIP3(AmazfitBip3Coordinator.class),
    AMAZFITBIP3PRO(AmazfitBip3ProCoordinator.class),
    AMAZFITCHEETAHPRO(AmazfitCheetahProCoordinator.class),
    AMAZFITCHEETAHSQUARE(AmazfitCheetahSquareCoordinator.class),
    AMAZFITCHEETAHROUND(AmazfitCheetahRoundCoordinator.class),
    AMAZFITBIP5(AmazfitBip5Coordinator.class),
    AMAZFITBIP5UNITY(AmazfitBip5UnityCoordinator.class),
    AMAZFITBIP6(AmazfitBip6Coordinator.class),
    AMAZFITTREXULTRA(AmazfitTRexUltraCoordinator.class),
    AMAZFITGTRMINI(AmazfitGTRMiniCoordinator.class),
    AMAZFITFALCON(AmazfitFalconCoordinator.class),
    AMAZFITBALANCE(AmazfitBalanceCoordinator.class),
    AMAZFITBALANCE2(AmazfitBalance2Coordinator.class),
    AMAZFITACTIVE(AmazfitActiveCoordinator.class),
    AMAZFITACTIVE2(AmazfitActive2RoundCoordinator.class),
    AMAZFITACTIVE2NFC(AmazfitActive2NfcCoordinator.class),
    AMAZFITACTIVE2SQUARE(AmazfitActive2SquareCoordinator.class),
    AMAZFITACTIVEEDGE(AmazfitActiveEdgeCoordinator.class),
    AMAZFITHELIORING(AmazfitHelioRingCoordinator.class),
    AMAZFITHELIOSTRAP(AmazfitHelioStrapCoordinator.class),
    HPLUS(HPlusCoordinator.class),
    MAKIBESF68(MakibesF68Coordinator.class),
    EXRIZUK8(EXRIZUK8Coordinator.class),
    Q8(Q8Coordinator.class),
    SG2(SG2Coordinator.class),
    NO1F1(No1F1Coordinator.class),
    TECLASTH30(TeclastH30Coordinator.class),
    Y5(Y5Coordinator.class),
    XWATCH(XWatchCoordinator.class),
    ZETIME(ZeTimeCoordinator.class),
    ID115(ID115Coordinator.class),
    WATCH9(Watch9DeviceCoordinator.class),
    WATCHXPLUS(WatchXPlusDeviceCoordinator.class),
    ROIDMI(Roidmi1Coordinator.class),
    ROIDMI3(Roidmi3Coordinator.class),
    CASIOECBS100(CasioECBS100DeviceCoordinator.class),

    CASIOGB6900(CasioGB6900DeviceCoordinator.class),
    CASIOGBX100(CasioGBX100DeviceCoordinator.class),
    CASIOGWB5600(CasioGWB5600DeviceCoordinator.class),
    CASIOGMWB5000(CasioGMWB5000DeviceCoordinator.class),
    MISMARTSCALE(MiSmartScaleCoordinator.class),
    MICOMPOSITIONSCALE(MiCompositionScaleCoordinator.class),
    BFH16(BFH16DeviceCoordinator.class),
    MAKIBESHR3(MakibesHR3Coordinator.class),
    BANGLEJS(BangleJSCoordinator.class),
    FOSSILQHYBRID(QHybridCoordinator.class),
    TLW64(TLW64Coordinator.class),
    PINETIME_JF(PineTimeJFCoordinator.class),
    POLARH9(PolarH9DeviceCoordinator.class),
    POLARH10(PolarH10DeviceCoordinator.class),
    MIJIA_LYWSD02(MijiaLywsd02Coordinator.class),
    MIJIA_LYWSD03(MijiaLywsd03Coordinator.class),
    MIJIA_XMWSDJ04(MijiaXmwsdj04Coordinator.class),
    MIJIA_MHO_C303(MijiaMhoC303Coordinator.class),
    LEFUN(LefunDeviceCoordinator.class),
    VIVITAR_HR_BP_MONITOR_ACTIVITY_TRACKER(VivitarHrBpMonitorActivityTrackerCoordinator.class),
    BOHEMIC_SMART_BRACELET(BohemicSmartBraceletDeviceCoordinator.class),
    SMAQ2OSS(SMAQ2OSSCoordinator.class),
    FITPRO(FitProDeviceCoordinator.class),
    COLACAO21(ColaCao21Coordinator.class),
    COLACAO23(ColaCao23Coordinator.class),
    ITAG(ITagCoordinator.class),
    IKEA_IDASEN(IdasenCoordinator.class),
    NUTMINI(NutCoordinator.class),
    VIVOMOVE_HR(GarminVivomoveHrCoordinator.class),
    GARMIN_HRM_PRO_PLUS(GarminHrmProPlusCoordinator.class),
    GARMIN_HRM_200(GarminHrm200Coordinator.class),
    GARMIN_EDGE_130_PLUS(GarminEdge130PlusCoordinator.class),
    GARMIN_EDGE_540(GarminEdge540Coordinator.class),
    GARMIN_EDGE_EXPLORE_2(GarminEdgeExplore2Coordinator.class),
    GARMIN_ETREX_SE(GarminETrexSeCoordinator.class),
    GARMIN_ENDURO_3(GarminEnduro3Coordinator.class),
    GARMIN_EPIX_PRO(GarminEpixProCoordinator.class),
    GARMIN_DESCENT_MK3(GarminDescentMk3Coordinator.class),
    GARMIN_FENIX_3(GarminFenix3Coordinator.class),
    GARMIN_FENIX_3_HR(GarminFenix3HrCoordinator.class),
    GARMIN_FENIX_5(GarminFenix5Coordinator.class),
    GARMIN_FENIX_5_PLUS(GarminFenix5PlusCoordinator.class),
    GARMIN_FENIX_5X_PLUS(GarminFenix5XPlusCoordinator.class),
    GARMIN_FENIX_6(GarminFenix6Coordinator.class),
    GARMIN_FENIX_6_SAPPHIRE(GarminFenix6SapphireCoordinator.class),
    GARMIN_FENIX_6S_PRO(GarminFenix6SProCoordinator.class),
    GARMIN_FENIX_6S_SAPPHIRE(GarminFenix6SSapphireCoordinator.class),
    GARMIN_FENIX_6X_PRO(GarminFenix6XProCoordinator.class),
    GARMIN_FENIX_6X_PRO_SOLAR(GarminFenix6XProSolarCoordinator.class),
    GARMIN_FENIX_7(GarminFenix7Coordinator.class),
    GARMIN_FENIX_7S(GarminFenix7SCoordinator.class),
    GARMIN_FENIX_7S_PRO(GarminFenix7SProCoordinator.class),
    GARMIN_FENIX_7X(GarminFenix7XCoordinator.class),
    GARMIN_FENIX_7_PRO(GarminFenix7ProCoordinator.class),
    GARMIN_FENIX_7X_PRO(GarminFenix7XProCoordinator.class),
    GARMIN_FENIX_8(GarminFenix8Coordinator.class),
    GARMIN_FORERUNNER_45(GarminForerunner45Coordinator.class),
    GARMIN_FORERUNNER_55(GarminForerunner55Coordinator.class),
    GARMIN_FORERUNNER_165(GarminForerunner165Coordinator.class),
    GARMIN_FORERUNNER_165_MUSIC(GarminForerunner165MusicCoordinator.class),
    GARMIN_FORERUNNER_235(GarminForerunner235Coordinator.class),
    GARMIN_FORERUNNER_245(GarminForerunner245Coordinator.class),
    GARMIN_FORERUNNER_245_MUSIC(GarminForerunner245MusicCoordinator.class),
    GARMIN_FORERUNNER_255(GarminForerunner255Coordinator.class),
    GARMIN_FORERUNNER_255_MUSIC(GarminForerunner255MusicCoordinator.class),
    GARMIN_FORERUNNER_255S(GarminForerunner255SCoordinator.class),
    GARMIN_FORERUNNER_255S_MUSIC(GarminForerunner255SMusicCoordinator.class),
    GARMIN_FORERUNNER_265(GarminForerunner265Coordinator.class),
    GARMIN_FORERUNNER_265S(GarminForerunner265SCoordinator.class),
    GARMIN_FORERUNNER_620(GarminForerunner620Coordinator.class),
    GARMIN_FORERUNNER_630(GarminForerunner630Coordinator.class),
    GARMIN_FORERUNNER_645_MUSIC(GarminForerunner645MusicCoordinator.class),
    GARMIN_FORERUNNER_735XT(GarminForerunner735XTCoordinator.class),
    GARMIN_FORERUNNER_945(GarminForerunner945Coordinator.class),
    GARMIN_FORERUNNER_955(GarminForerunner955Coordinator.class),
    GARMIN_FORERUNNER_965(GarminForerunner965Coordinator.class),
    GARMIN_SWIM_2(GarminSwim2Coordinator.class),
    GARMIN_INREACH_MINI_2(GarminInReachMini2.class),
    GARMIN_INSTINCT(GarminInstinctCoordinator.class),
    GARMIN_INSTINCT_SOLAR(GarminInstinctSolarCoordinator.class),
    GARMIN_INSTINCT_TACTICAL(GarminInstinctTacticalCoordinator.class),
    GARMIN_INSTINCT_2(GarminInstinct2Coordinator.class),
    GARMIN_INSTINCT_2S(GarminInstinct2SCoordinator.class),
    GARMIN_INSTINCT_2S_SOLAR(GarminInstinct2SSolarCoordinator.class),
    GARMIN_INSTINCT_2X_SOLAR(GarminInstinct2XSolarCoordinator.class),
    GARMIN_INSTINCT_2X_TACTICAL(GarminInstinct2XTacticalCoordinator.class),
    GARMIN_INSTINCT_2_SOLAR(GarminInstinct2SolarCoordinator.class),
    GARMIN_INSTINCT_2_SOLTAC(GarminInstinct2SolTacCoordinator.class),
    GARMIN_INSTINCT_CROSSOVER(GarminInstinctCrossoverCoordinator.class),
    GARMIN_INSTINCT_E(GarminInstinctECoordinator.class),
    GARMIN_INSTINCT_3(GarminInstinct3Coordinator.class),
    GARMIN_INSTINCT_3_TACTICAL(GarminInstinct3TacticalCoordinator.class),
    GARMIN_LILY_2_ACTIVE(GarminLily2ActiveCoordinator.class),
    GARMIN_VIVOMOVE_SPORT(GarminVivomoveSportCoordinator.class),
    GARMIN_VIVOMOVE_STYLE(GarminVivomoveStyleCoordinator.class),
    GARMIN_VIVOMOVE_TREND(GarminVivomoveTrendCoordinator.class),
    GARMIN_VENU(GarminVenuCoordinator.class),
    GARMIN_VENU_SQ(GarminVenuSqCoordinator.class),
    GARMIN_VENU_SQ_2(GarminVenuSq2Coordinator.class),
    GARMIN_VENU_2(GarminVenu2Coordinator.class),
    GARMIN_VENU_2S(GarminVenu2SCoordinator.class),
    GARMIN_VENU_2_PLUS(GarminVenu2PlusCoordinator.class),
    GARMIN_VENU_3(GarminVenu3Coordinator.class),
    GARMIN_VENU_3S(GarminVenu3SCoordinator.class),
    GARMIN_VENU_X1(GarminVenuX1Coordinator.class),
    GARMIN_VIVOACTIVE_3(GarminVivoActive3Coordinator.class),
    GARMIN_VIVOACTIVE_4(GarminVivoActive4Coordinator.class),
    GARMIN_VIVOACTIVE_4S(GarminVivoActive4SCoordinator.class),
    GARMIN_VIVOACTIVE_5(GarminVivoActive5Coordinator.class),
    GARMIN_VIVOACTIVE_6(GarminVivoActive6Coordinator.class),
    GARMIN_VIVOACTIVE_HR(GarminVivoActiveHrCoordinator.class),
    GARMIN_VIVOSMART_3(GarminVivosmart3Coordinator.class),
    GARMIN_VIVOSMART_4(GarminVivosmart4Coordinator.class),
    GARMIN_VIVOSMART_5(GarminVivosmart5Coordinator.class),
    GARMIN_VIVOSPORT(GarminVivosportCoordinator.class),
    GREE_AC(GreeAcCoordinator.class),
    AAWIRELESS(AAWirelessCoordinator.class),
    VIBRATISSIMO(VibratissimoCoordinator.class),
    SONY_SWR12(SonySWR12DeviceCoordinator.class),
    LIVEVIEW(LiveviewCoordinator.class),
    WASPOS(WaspOSCoordinator.class),
    UM25(UM25Coordinator.class),
    DOMYOS_T540(DomyosT540Coordinator.class),
    EVEN_REALITIES_G_1(G1DeviceCoordinator.class),
    NOTHING_EAR1(Ear1Coordinator.class),
    NOTHING_EAR2(Ear2Coordinator.class),
    NOTHING_EAR_A(EarACoordinator.class),
    NOTHING_EAR_STICK(EarStickCoordinator.class),
    NOTHING_CMF_BUDS_PRO_2(CmfBudsPro2Coordinator.class),
    NOTHING_CMF_WATCH_PRO(CmfWatchProCoordinator.class),
    NOTHING_CMF_WATCH_PRO_2(CmfWatchPro2Coordinator.class),
    GALAXY_BUDS_PRO(GalaxyBudsProDeviceCoordinator.class),
    GALAXY_BUDS_LIVE(GalaxyBudsLiveDeviceCoordinator.class),
    GALAXY_BUDS(GalaxyBudsDeviceCoordinator.class),
    GALAXY_BUDS2(GalaxyBuds2DeviceCoordinator.class),
    GALAXY_BUDS2_PRO(GalaxyBuds2ProDeviceCoordinator.class),
    SONY_WH_1000XM3(SonyWH1000XM3Coordinator.class),
    SONY_WF_SP800N(SonyWFSP800NCoordinator.class),
    SONY_WI_C100(SonyWIC100Coordinator.class),
    SONY_WI_SP600N(SonyWISP600NCoordinator.class),
    SONY_WH_1000XM4(SonyWH1000XM4Coordinator.class),
    SONY_WF_1000XM3(SonyWF1000XM3Coordinator.class),
    SONY_WH_1000XM2(SonyWH1000XM2Coordinator.class),
    SONY_WF_1000XM4(SonyWF1000XM4Coordinator.class),
    SONY_LINKBUDS(SonyLinkBudsCoordinator.class),
    SONY_LINKBUDS_S(SonyLinkBudsSCoordinator.class),
    SONY_WH_1000XM5(SonyWH1000XM5Coordinator.class),
    SONY_WH_ULT900N(SonyWHULT900NCoordinator.class),
    SONY_WF_1000XM5(SonyWF1000XM5Coordinator.class),
    SONY_WF_C500(SonyWFC500Coordinator.class),
    SONY_WF_C510(SonyWFC510Coordinator.class),
    SONY_WF_C700N(SonyWFC700NCoordinator.class),
    SONY_WF_C710N(SonyWFC710NCoordinator.class),
    PIXEL_BUDS_A(PixelBudsACoordinator.class),
    SHOKZ_OPENSWIM_PRO(ShokzOpenSwimProCoordinator.class),
    SOUNDCORE_LIBERTY3_PRO(SoundcoreLiberty3ProCoordinator.class),
    SOUNDCORE_LIBERTY4_NC(SoundcoreLiberty4NCCoordinator.class),
    SOUNDCORE_MOTION300(SoundcoreMotion300Coordinator.class),
    SOUNDCORE_Q30(SoundcoreQ30Coordinator.class),
    SOUNDCORE_AEROFIT2(SoundcoreAeroFit2Coordinator.class),
    MOONDROP_SPACE_TRAVEL(MoondropSpaceTravelCoordinator.class),
    BOSE_QC35(QC35Coordinator.class),
    ONEMORE_SONOFLOW(OneMoreSonoFlowCoordinator.class),
    HONORBAND3(HonorBand3Coordinator.class),
    HONORBAND4(HonorBand4Coordinator.class),
    HONORBAND5(HonorBand5Coordinator.class),
    HUAWEIBANDAW70(HuaweiBandAw70Coordinator.class),
    HUAWEIBAND6(HuaweiBand6Coordinator.class),
    HUAWEIWATCHGT(HuaweiWatchGTCoordinator.class),
    HUAWEIBAND2PRO(HuaweiBand2ProCoordinator.class),
    HUAWEIBAND3PRO(HuaweiBand3ProCoordinator.class),
    HUAWEIBAND4PRO(HuaweiBand4ProCoordinator.class),
    HUAWEIWATCHGT2(HuaweiWatchGT2Coordinator.class),
    HUAWEIWATCHGT2E(HuaweiWatchGT2eCoordinator.class),
    HUAWEITALKBANDB6(HuaweiTalkBandB6Coordinator.class),
    HUAWEIBAND7(HuaweiBand7Coordinator.class),
    HONORBAND6(HonorBand6Coordinator.class),
    HONORBAND7(HonorBand7Coordinator.class),
    HONORWATCH4(HonorWatch4Coordinator.class),
    HONORMAGICWATCH2(HonorMagicWatch2Coordinator.class),
    HONORWATCHGS3(HonorWatchGS3Coordinator.class),
    HONORWATCHGSPRO(HonorWatchGSProCoordinator.class),
    HUAWEIWATCHD2(HuaweiWatchD2Coordinator.class),
    HUAWEIWATCHGT3(HuaweiWatchGT3Coordinator.class),
    HUAWEIWATCHGT4(HuaweiWatchGT4Coordinator.class),
    HUAWEIWATCHGT5(HuaweiWatchGT5Coordinator.class),
    HUAWEIWATCHGT6(HuaweiWatchGT6Coordinator.class),
    HUAWEIWATCHGTRUNNER(HuaweiWatchGTRunnerCoordinator.class),
    HUAWEIWATCHGTCYBER(HuaweiWatchGTCyberCoordinator.class),
    HUAWEIBAND8(HuaweiBand8Coordinator.class),
    HUAWEIBAND9(HuaweiBand9Coordinator.class),
    HUAWEIBAND10(HuaweiBand10Coordinator.class),
    HUAWEIWATCHFIT(HuaweiWatchFitCoordinator.class),
    HUAWEIWATCHFIT2(HuaweiWatchFit2Coordinator.class),
    HUAWEIWATCHFIT3(HuaweiWatchFit3Coordinator.class),
    HUAWEIWATCHFIT4PRO(HuaweiWatchFit4ProCoordinator.class),
    HUAWEIWATCHULTIMATE(HuaweiWatchUltimateCoordinator.class),
    HUAWEIWATCH3(HuaweiWatch3Coordinator.class),
    HUAWEIWATCH4PRO(HuaweiWatch4ProCoordinator.class),
    HUAWEIWATCH5(HuaweiWatch5Coordinator.class),
    HUAWEI_FREEARC(HuaweiFreearcCoordinator.class),
    HUAWEI_FREEBUDS5I(HuaweiFreebuds5iCoordinator.class),
    HUAWEI_FREEBUDS_PRO(HuaweiFreebudsProCoordinator.class),
    HUAWEI_FREEBUDS6(HuaweiFreebuds6Coordinator.class),
    VESC(VescCoordinator.class),
    BINARY_SENSOR(BinarySensorCoordinator.class),
    FLIPPER_ZERO(FlipperZeroCoordinator.class),
    SUPER_CARS(SuperCarsCoordinator.class),
    ASTEROIDOS(AsteroidOSDeviceCoordinator.class),
    OPPO_ENCO_AIR(OppoEncoAirCoordinator.class),
    OPPO_ENCO_AIR2(OppoEncoAir2Coordinator.class),
    OPPO_ENCO_BUDS2(OppoEncoBuds2Coordinator.class),
    OUKITEL_BT103(OukitelBT103Coordinator.class),
    DOTN_P66D(DotnP66DCoordinator.class),
    HAYLOU_WATCH_2_PRO(HaylouWatch2ProCoordinator.class),
    Y66(Y66Coordinator.class),
    REALME_BUDS_T110(RealmeBudsT110Coordinator.class),
    REALME_BUDS_T100(RealmeBudsT100Coordinator.class),
    REALME_BUDS_T300(RealmeBudsT300Coordinator.class),
    REALME_BUDS_AIR_5_PRO(RealmeBudsAir5ProCoordinator.class),
    SOFLOW_SO6(SoFlowCoordinator.class),
    WITHINGS_STEEL_HR(WithingsSteelHRDeviceCoordinator.class),
    SONY_WENA_3(SonyWena3Coordinator.class),
    FEMOMETER_VINCA2(FemometerVinca2DeviceCoordinator.class),
    PIXOO(PixooCoordinator.class),
    HAMA_FIT6900(HamaFit6900DeviceCoordinator.class),
    YAWELL_R10(YawellR10Coordinator.class),
    YAWELL_R11(YawellR11Coordinator.class),
    COLMI_R02(ColmiR02Coordinator.class),
    COLMI_R03(ColmiR03Coordinator.class),
    COLMI_R06(ColmiR06Coordinator.class),
    COLMI_R07(ColmiR07Coordinator.class),
    COLMI_R09(ColmiR09Coordinator.class),
    COLMI_R10(ColmiR10Coordinator.class),
    COLMI_R12(ColmiR12Coordinator.class),
    H59(H59Coordinator.class),
    COLMI_I28_ULTRA(ColmiI28UltraCoordinator.class),
    COLMI_P76(ColmiP76Coordinator.class),
    COLMI_P80(ColmiP80Coordinator.class),
    COLMI_V72(ColmiV72Coordinator.class),
    COLMI_V76(ColmiV76Coordinator.class),
    COLMI_V89(ColmiV89Coordinator.class),
    BLACKVIEW_R60(BlackviewR60Coordinator.class),
    MISIRUN_C17(MisirunC17Coordinator.class),
    C20(C20Coordinator.class),
    HC96(HC96Coordinator.class),
    KT80(KT80Coordinator.class),
    KSIX_VENTURE(KsixVentureCoordinator.class),
    FIREBOLTT_TALK(FireBolttTalkCoordinator.class),
    VIRAN_C29(ViranC29Coordinator.class),
    R50PRO(R50ProCoordinator.class),
    RAINBUVVY_T97(RainbuvvyT97Coordinator.class),
    ADVAN_WATCH_SE1_AI(AdvanWatchSE1AICoordinator.class),
    B_AND_W_P_SERIES(BandWPSeriesDeviceCoordinator.class),
    EARFUN_AIR_S(EarFunAirSCoordinator.class),
    EARFUN_AIR_PRO_4(EarFunAirPro4Coordinator.class),
    SCANNABLE(ScannableDeviceCoordinator.class),
    CYCLING_SENSOR(CyclingSensorCoordinator.class),
    BLE_GATT_CLIENT(BleGattClientCoordinator.class),
    MARSTEK_B2500(MarstekB2500DeviceCoordinator.class),
    GENERIC_HEADPHONES(GenericHeadphonesCoordinator.class),
    GENERIC_THERMAL_PRINTER(ThermalPrinterCoordinator.class),
    GENERIC_HEART_RATE(GenericHeartRateCoordinator.class),
    ULTRAHUMAN_RING_AIR(UltrahumanDeviceCoordinator.class),
    ATC_BLE_OEPL(ATCBLEOEPLCoordinator.class),
    COOSPO_H6(CoospoH6Coordinator.class),
    COOSPO_HW9(CoospoHW9Coordinator.class),
    COOSPO_HW807(CoospoHW807Coordinator.class),
    GENERIC_WEIGHT_SCALE(GenericWeightScaleCoordinator.class),
    TEST(TestDeviceCoordinator.class);

    private DeviceCoordinator coordinator;

    private Class<? extends DeviceCoordinator> coordinatorClass;

    DeviceType(Class<? extends DeviceCoordinator> coordinatorClass) {
        this.coordinatorClass = coordinatorClass;
    }

    public boolean isSupported() {
        return this != UNKNOWN;
    }

    public static DeviceType fromName(String name) {
        for (DeviceType type : values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return DeviceType.UNKNOWN;
    }

    public DeviceCoordinator getDeviceCoordinator() {
        if (coordinator == null) {
            try {
                coordinator = coordinatorClass.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        return coordinator;
    }
}
