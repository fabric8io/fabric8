package archetype

#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )

package ${package};

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p></p>
 *
 * @author Grzegorz Grzybek
 */
public class AppTest
{
    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    @Test
    public void justLog()
    {
        log.info("SLF4J + Logback dzia${symbol_escape}u0142a");
    }
}
