package its_meow.claimitdynmap;

import its_meow.claimit.api.userconfig.UserConfigType;
import its_meow.claimit.api.util.nbt.NBTDeserializer;
import its_meow.claimit.api.util.nbt.NBTSerializer;

public class UserConfigTypeColor extends UserConfigType<String> {

    public UserConfigTypeColor() {
        super(String.class);
    }
    
    @Override
    protected NBTSerializer<String> getSerializer() {
        return (c, s, v) -> c.setString(s, (String) v);
    }

    @Override
    protected NBTDeserializer<String> getDeserializer() {
        return (c, s) -> c.getString(s);
    }

    @Override
    public boolean isValidValue(String in) {
        if(in.matches("0x[0-9a-fA-F]{6}")) {
            try {
                Integer.parseInt(in.substring(2), 16);
            } catch(NumberFormatException e) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String fromString(String valueStr) {
        return valueStr;
    }

}
