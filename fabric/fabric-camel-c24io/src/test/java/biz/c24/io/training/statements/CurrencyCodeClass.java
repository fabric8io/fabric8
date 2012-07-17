package biz.c24.io.training.statements;


/**
 * The CurrencyCode atomic simple data type.
 * 
 * @author C24 Integration Objects;
 **/
public class CurrencyCodeClass extends biz.c24.io.api.data.GenericStringDataType 
{
    protected static CurrencyCodeClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected CurrencyCodeClass()
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
            synchronized (biz.c24.io.training.statements.CurrencyCodeClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.training.statements.CurrencyCodeClass();
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
        setName("CurrencyCode");
        setModel(biz.c24.io.training.statements.StatementsDataModel.getInstance());
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

}
