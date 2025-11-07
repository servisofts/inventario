package Util;

import Servisofts.SConfig;
import Servisofts.SPGConectInstance;

public class ConectInstance extends SPGConectInstance{

    public ConectInstance() {
        super(SConfig.getJSON("data_base"));
    }

    public void close() {
        try {
            this.getConexion().close();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
    
}
