package biz.c24.io.training.statements;


/**
 * The Max16Text atomic simple data type.
 * 
 * @author C24 Integration Objects;
 **/
public class Max16TextClass extends biz.c24.io.api.data.GenericStringDataType 
{
    protected static Max16TextClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected Max16TextClass()
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
            synchronized (biz.c24.io.training.statements.Max16TextClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.training.statements.Max16TextClass();
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
        setName("Max16Text");
        setModel(biz.c24.io.training.statements.StatementsDataModel.getInstance());
        setMinLength(1);
        setMaxLength(16);
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

}
