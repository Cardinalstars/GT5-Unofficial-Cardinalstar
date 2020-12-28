package gregtech.api.enums;

import net.minecraft.util.EnumChatFormatting;

public enum HeatingCoilLevel {
        None, //                       0
        ULV,  //Not implemented      901
        LV,   //Cupronickel         1801
        MV,   //KANTHAL             2701
        HV,   //NICHROME            3601
        EV,   //TUNGSTENSTEEL       4501
        IV,   //HSSG                5401
        LuV,  //HSSS                6301
        ZPM,  //NAQUADAH            7201
        UV,   //NAQUADAHALLOY       8101
        UHV,  //TRINIUM             9001
        UEV,  //ELECTRUMFLUX        9901
        UIV,  //AWAKENEDDRACONIUM  10801
        //Not Implemented yet
        UMV,
        UXV,
        OpV,
        MAX,
        ;

        /**
         * @return the Coils Tier Name
         */
        public String getTierName() {
            if (this.ordinal() < 1 || (this.ordinal()-1) >= GT_Values.VN.length)
                return "ERROR!";
            return GT_Values.TIER_COLORS[this.ordinal() - 1] + GT_Values.VOLTAGE_NAMES[this.ordinal() - 1] + EnumChatFormatting.RESET;
        }

        /**
         * @return the coil heat, used for recipes in the Electronic Blast Furnace for example
         */
        public long getHeat() {
            return this == None ? 0 : 1L + (900L * this.ordinal());
        }

        /**
         * @return the coil tier, used for discount in the Pyrolyse Ofen for example
         */
        public byte getTier() {
            return (byte) (this.ordinal() - 2);
        }

        /**
         * @return the coil Level, used for Parallels in the Multi Furnace for example
         */
        public byte getLevel() {
            return (byte) Math.max(16, 2 << (this.ordinal() - 2));
        }

        /**
         * @return the coil Discount, used for discount in the Multi Furnace for example
         */
        public byte getCostDiscount() {
            return (byte) Math.min(1, 2 << (this.ordinal() - 1 - 6)); //-1 bcs. of none, -4 = offset
        }

        public static HeatingCoilLevel getFromTier(byte tier){
            if (tier < 0 || tier > HeatingCoilLevel.values().length -1)
                return HeatingCoilLevel.None;

            return HeatingCoilLevel.values()[tier+2];
        }
}