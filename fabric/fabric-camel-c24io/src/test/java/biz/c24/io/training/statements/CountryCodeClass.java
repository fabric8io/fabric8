package biz.c24.io.training.statements;


/**
 * The CountryCode atomic simple data type.
 * 
 * @author C24 Integration Objects;
 **/
public class CountryCodeClass extends biz.c24.io.api.data.GenericStringDataType 
{
    protected static CountryCodeClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected CountryCodeClass()
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
            synchronized (biz.c24.io.training.statements.CountryCodeClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.training.statements.CountryCodeClass();
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
        setName("CountryCode");
        setModel(biz.c24.io.training.statements.StatementsDataModel.getInstance());
        setPatternType(biz.c24.io.api.data.PatternTypeEnum.SCHEMA);
        setPatternMatch("[A-Z]{2,2}");
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

}
