package biz.c24.io.training.statements;


/**
 * The Max5Integer atomic simple data type.
 * 
 * @author C24 Integration Objects;
 **/
public class Max5IntegerClass extends biz.c24.io.api.data.IntegerDataType 
{
    protected static Max5IntegerClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected Max5IntegerClass()
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
            synchronized (biz.c24.io.training.statements.Max5IntegerClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.training.statements.Max5IntegerClass();
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
        setName("Max5Integer");
        setModel(biz.c24.io.training.statements.StatementsDataModel.getInstance());
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

}
