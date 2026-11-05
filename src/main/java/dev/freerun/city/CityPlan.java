package dev.freerun.city;

import dev.freerun.world.CityAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

import java.util.ArrayList;
import java.util.List;

/** Every piece of a city with its north-west corner (street level) at {@code city}. */
public final class CityPlan {
    private CityPlan() {}

    public static List<StructurePiece> pieces(BlockPos city) {
        List<StructurePiece> out = new ArrayList<>();
        // streets first, then the blocks
        for (int j = 0; j < CityLayout.ZS.length; j++) for (int i = 0; i < CityLayout.XS.length; i++) out.add(new IntersectionPiece(city, i, j));
        for (int line = 0; line < CityLayout.ZS.length; line++) for (int seg = 0; seg < CityLayout.cols(); seg++) out.add(new RoadPiece(city, true, line, seg));
        for (int line = 0; line < CityLayout.XS.length; line++) for (int seg = 0; seg < CityLayout.rows(); seg++) out.add(new RoadPiece(city, false, line, seg));
        for (int r = 0; r < CityLayout.rows(); r++) {
            for (int c = 0; c < CityLayout.cols(); c++) {
                int lx = CityLayout.lotX(c), lz = CityLayout.lotZ(r), w = CityLayout.lotW(c), d = CityLayout.lotD(r);
                BlockPos lot = city.offset(lx, 0, lz);
                switch (CityLayout.district(c, r)) {
                    case SUBURB -> out.add(new SuburbPiece(city, lx, lz, w, d));
                    case OFFICE -> out.add(new OfficePiece(city, lx, lz, w, d));
                    case PARK -> out.add(new ParkPiece(city, lx, lz, w, d));
                    case HOOD -> out.add(new HoodPiece(city, lx, lz, w, d));
                    case ROADHOUSE -> {
                        out.add(new LotPiece(city, lx, lz, 38, d, 16, LotPiece.GRASS));
                        out.add(new LotPiece(city, lx + 38, lz, w - 38, d, 16, LotPiece.PARKING));
                        out.add(CityAccess.roadhouse(lot.offset(3, 0, 8)));
                    }
                    case GENERAL_STORE -> {
                        out.add(new LotPiece(city, lx, lz, w, d, 16, LotPiece.GRASS));
                        out.add(CityAccess.generalStore(lot.offset(10, 0, 4)));
                    }
                    case DOUBLE_QUICK -> {
                        out.add(new LotPiece(city, lx, lz, w, 30, 16, LotPiece.GRASS));
                        out.add(new LotPiece(city, lx, lz + 32, w, d - 32, 16, LotPiece.PARKING));
                        out.add(CityAccess.doubleQuick(lot.offset(1, 0, 3)));
                    }
                    case FACTORY -> {
                        out.add(new LotPiece(city, lx, lz, w, d, 20, LotPiece.GRASS));
                        out.addAll(CityAccess.factory(lot.offset(20, 0, 3 + CityAccess.LOT_DEPTH)));
                    }
                }
            }
        }
        return out;
    }
}
