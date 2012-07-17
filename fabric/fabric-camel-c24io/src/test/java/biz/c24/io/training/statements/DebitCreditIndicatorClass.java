package biz.c24.io.training.statements;


/**
 * The DebitCreditIndicator atomic simple data type.
 * 
 * @author C24 Integration Objects;
 **/
public class DebitCreditIndicatorClass extends biz.c24.io.api.data.GenericStringDataType 
{
    protected static DebitCreditIndicatorClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected DebitCreditIndicatorClass()
    {
    }

    /**
     * Gets the singleton instance of this type.
     * @return The type, or its supertype if present.
     **/
    public static biz.c24.io.api.data.DataType getInstance()
    {
        if (!initialized)
        {
            synchronized (biz.c24.io.training.statements.DebitCreditIndicatorClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.training.statements.DebitCreditIndicatorClass();
                    instance.init();
                    initialized = true;
                }
            }
        }
        return instance;
    }

    /**
     * Called internally to initialize this type.
     **/
    protected void init()
    {
        setName("DebitCreditIndicator");
        setModel(biz.c24.io.training.statements.StatementsDataModel.getInstance());
        addValidator(biz.c24.io.training.statements.DebitCreditIndicatorClass.DebitCreditIndicator1Enum.getInstance());
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

    /**
     * The DebitCreditIndicator enumeration.
     * 
     * @author C24 Integration Objects;
     **/
    public static class DebitCreditIndicator1Enum extends biz.c24.io.api.data.DefaultEnumeration 
    {
        private static biz.c24.io.api.data.Enumeration instance;

        private DebitCreditIndicator1Enum()
        {
            setName("DebitCreditIndicator");
            addEntry("CR", "", null);
            addEntry("DR", "", null);
        }

        public static biz.c24.io.api.data.Enumeration getInstance()
        {
            if (instance == null)
                instance = new biz.c24.io.training.statements.DebitCreditIndicatorClass.DebitCreditIndicator1Enum();
            
            return instance;
        }

    }

}
