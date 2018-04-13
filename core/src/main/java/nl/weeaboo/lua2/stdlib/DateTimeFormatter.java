package nl.weeaboo.lua2.stdlib;

import java.text.CharacterIterator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class DateTimeFormatter {

    /**
     * Java imitation of the {@code strftime} in C. C99 extensions aren't supported.
     */
    public String strftime(String format, Date instant, TimeZone timeZone) {
        return new Strftime(instant, timeZone).format(format);
    }

    private static final class Strftime {

        private final Date instant;
        private final TimeZone timeZone;

        public Strftime(Date instant, TimeZone timeZone) {
            this.instant = instant;
            this.timeZone = timeZone;
        }

        public String format(String format) {
            StringBuilder out = new StringBuilder();
            StringCharacterIterator itr = new StringCharacterIterator(format);
            while (itr.current() != CharacterIterator.DONE) {
                char c = itr.current();
                if (c != '%') {
                    // Regular character
                    out.append(c);
                } else {
                    // Handle format of form "%X"
                    c = itr.next();
                    out.append(handleToken(c));
                }
                itr.next();
            }
            return out.toString();
        }

        private String handleToken(char specifier) {
            switch (specifier) {
            case 'a': // Abbreviated weekday name
                return simpleDateFormat("EEE");
            case 'A': // Full weekday name
                return simpleDateFormat("E");
            case 'b': // Abbreviated month name
                return simpleDateFormat("MMM");
            case 'B': // Full month name
                return simpleDateFormat("M");
            case 'c': { // Date and time representation
                DateFormat format = SimpleDateFormat.getDateInstance(DateFormat.DEFAULT, getLocale());
                format.setTimeZone(timeZone);
                return format.format(instant);
            }
            case 'd': // Day of the month, zero-padded (01-31)
                return simpleDateFormat("dd");
            case 'H': // Hour in 24h format (00-23)
                return simpleDateFormat("HH");
            case 'I': // Hour in 12h format (01-12)
                return simpleDateFormat("hh");
            case 'j': // Day of the year (001-366)
                return simpleDateFormat("DDD");
            case 'm': // Month as a decimal number (01-12)
                return simpleDateFormat("MM");
            case 'M': // Minute (00-59)
                return simpleDateFormat("mm");
            case 'p': // AM or PM designation
                return simpleDateFormat("a");
            case 'S': // Second (00-61)
                return simpleDateFormat("ss");
            case 'U': // Week number with the first Sunday as the first day of week one (00-53)
                return simpleDateFormat("w"); // May not always return correct result, depending on Locale
            case 'w': // Weekday as a decimal number with Sunday as 0 (0-6)
                return Integer.toString(getCalendar().get(Calendar.DAY_OF_WEEK) - 1);
            case 'W': // Week number with the first Monday as the first day of week one (00-53)
                return simpleDateFormat("w"); // May not always return correct result, depending on Locale
            case 'x': { // Date representation
                DateFormat format = SimpleDateFormat.getDateInstance(DateFormat.SHORT, getLocale());
                format.setTimeZone(timeZone);
                return format.format(instant);
            }
            case 'X': { // Time representation
                DateFormat format = SimpleDateFormat.getTimeInstance(DateFormat.SHORT, getLocale());
                format.setTimeZone(timeZone);
                return format.format(instant);
            }
            case 'y': // Year, last two digits (00-99)
                return simpleDateFormat("yy");
            case 'Y': // Year
                return simpleDateFormat("yyyy");
            case 'Z': // Timezone name or abbreviation
                return simpleDateFormat("zzz");
            default:
                return String.valueOf(specifier);
            }
        }

        private String simpleDateFormat(String pattern) {
            SimpleDateFormat format = new SimpleDateFormat(pattern, getLocale());
            format.setTimeZone(timeZone);
            return format.format(instant);
        }

        private Locale getLocale() {
            return Locale.getDefault();
        }

        private Calendar getCalendar() {
            Calendar cal = Calendar.getInstance(timeZone, getLocale());
            cal.setTime(instant);
            return cal;
        }

    }

}
