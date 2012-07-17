package biz.c24.io.training.statements;


/**
 * The StatementFile element.
 * 
 * @author C24 Integration Objects;
 **/
public class StatementFileElement extends biz.c24.io.api.data.Element 
{
    private static StatementFileElement instance;
    private static boolean initialized;

    public StatementFileElement()
    {
        this(true);
    }

    private StatementFileElement(boolean init)
    {
        super("StatementFile", 1, 1, biz.c24.io.training.statements.StatementFileClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance());
        
        if (init)
            init();
    }

    public static biz.c24.io.api.data.Element getInstance()
    {
        if (!initialized)
        {
            synchronized (biz.c24.io.training.statements.StatementFileElement.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.training.statements.StatementFileElement(false);
                    instance.init();
                    initialized = true;
                }
            }
        }
        return instance;
    }

    /**
     * Test method. Reads in a file (called <code>arg[0]</code>), parses it, prints the output to Standard.out and validates it.
     * @param args Should be of length one, where the first element is the filename to read from.
     **/
    public static void main(java.lang.String[] args)
    {
        if (args.length == 1)
        {
            biz.c24.io.api.data.Element element = biz.c24.io.training.statements.StatementFileElement.getInstance();
            org.apache.log4j.Appender appender = new org.apache.log4j.ConsoleAppender(new org.apache.log4j.SimpleLayout());
            element.getLog().addAppender(appender);
            element.getLog().setLevel(org.apache.log4j.Level.OFF); // change this to see console logging
            
            try
            {
                java.lang.System.out.println("Parsing... ");
                biz.c24.io.api.presentation.Source source = element.getModel().source();
                source.setInputStream(new java.io.FileInputStream(args[0]));
                biz.c24.io.api.data.ComplexDataObject bean = source.readObject(element);
                source.getInputStream().close();
                java.lang.System.out.println(bean.toString());
                
                java.lang.System.out.println("Validating... ");
                new biz.c24.io.api.data.ValidationManager().validateByException(bean);
                
                java.lang.System.out.println("Finished");
            }
            catch (java.lang.Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                element.getLog().removeAppender(appender);
            }
        }
        else
            java.lang.System.out.println("Usage: java biz.c24.io.training.statements.StatementFileElement <filename>");
    }

    private void init()
    {
    }

}
