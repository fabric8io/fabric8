package biz.c24.io.training.statements;


/**
 * The ISODate atomic simple data type.
 * 
 * @author C24 Integration Objects;
 **/
public class ISODateClass extends biz.c24.io.api.data.ISO8601DateDataType 
{
    protected static ISODateClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected ISODateClass()
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
            synchronized (biz.c24.io.training.statements.ISODateClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.training.statements.ISODateClass();
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
        setName("ISODate");
        setModel(biz.c24.io.training.statements.StatementsDataModel.getInstance());
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

}
