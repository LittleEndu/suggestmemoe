package moe.suggestme.scrapers;

import moe.suggestme.Runner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Endrik on 22-May-17.
 */
public class Helper {
    static int findId(String input){
        Pattern idPattern = Pattern.compile("/\\d+/?");
        Matcher m = idPattern.matcher(input);
        if (m.find()){
            return Integer.parseInt(m.group(0).replaceAll("/", ""));
        }
        return 0;
    }
}
