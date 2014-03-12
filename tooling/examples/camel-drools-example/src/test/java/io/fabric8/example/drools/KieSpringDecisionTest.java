package io.fabric8.example.drools;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.runtime.ExecutionResults;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class KieSpringDecisionTest {

    static ApplicationContext context = null;

    @BeforeClass
    public static void setup() {
        context = new ClassPathXmlApplicationContext("io/fabric/example/drools/beans.xml");
    }

    @Test
    public void testContext() throws Exception {
        assertNotNull(context);
    }

    @Test
    public void testKieBase() throws Exception {
        KieBase kbase = (KieBase) context.getBean("decisionCSV");
        assertNotNull(kbase);
    }

    @Test
    public void testStatelessKieSession() throws Exception {
        StatelessKieSession ksession = (StatelessKieSession) context.getBean("ksession-table-1");
        assertNotNull(ksession);

        // Cheeses selection
        Cheese cheese = new Cheese();
        cheese.setPrice(250);
        cheese.setType("cheddar");

        // Young person
        Person person = new Person();
        person.setName("Young Scott");
        person.setAge(21);

        List cmds = new ArrayList();
        cmds.add( CommandFactory.newSetGlobal("list", new ArrayList(), true) );
        cmds.add( CommandFactory.newInsert(person,"yscott"));
        cmds.add( CommandFactory.newInsert(cheese,"cheddar"));
        cmds.add( CommandFactory.newFireAllRules());

        // Execute the list
        ExecutionResults results = ksession.execute(CommandFactory.newBatchExecution(cmds));
        List list = (List) results.getValue("list");
        assertEquals(1, list.size());
        assertTrue(list.contains("Young man cheddar"));

        // Old person
        person = new Person();
        person.setName("Old Scott");
        person.setAge(42);

        cheese = new Cheese();
        cheese.setPrice(150);
        cheese.setType("stilton");

        cmds = new ArrayList();
        cmds.add( CommandFactory.newSetGlobal("list", new ArrayList(), true) );
        cmds.add( CommandFactory.newInsert(person,"oscott"));
        cmds.add( CommandFactory.newInsert(cheese,"stilton"));
        cmds.add( CommandFactory.newFireAllRules());

        // Execute the list
        results = ksession.execute(CommandFactory.newBatchExecution(cmds));
        list = (List) results.getValue("list");
        assertEquals(1, list.size());
        assertTrue(list.contains("Old man stilton"));

    }

    @AfterClass
    public static void tearDown() { }

}