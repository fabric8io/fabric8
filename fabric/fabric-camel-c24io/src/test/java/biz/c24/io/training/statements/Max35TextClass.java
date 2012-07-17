package biz.c24.io.training.statements;


/**
 * The Max35Text atomic simple data type.
 * 
 * @author C24 Integration Objects;
 **/
public class Max35TextClass extends biz.c24.io.api.data.GenericStringDataType 
{
    protected static Max35TextClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected Max35TextClass()
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
            synchronized (biz.c24.io.training.statements.Max35TextClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.training.statements.Max35TextClass();
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
        setName("Max35Text");
        setModel(biz.c24.io.training.statements.StatementsDataModel.getInstance());
        setMinLength(1);
        setMaxLength(35);
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

}
