package biz.c24.io.training.statements;


/**
 * The Max140Text atomic simple data type.
 * 
 * @author C24 Integration Objects;
 **/
public class Max140TextClass extends biz.c24.io.api.data.GenericStringDataType 
{
    protected static Max140TextClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected Max140TextClass()
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
            synchronized (biz.c24.io.training.statements.Max140TextClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.training.statements.Max140TextClass();
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
        setName("Max140Text");
        setModel(biz.c24.io.training.statements.StatementsDataModel.getInstance());
        setMinLength(1);
        setMaxLength(140);
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

}
