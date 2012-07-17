package biz.c24.io.training.statements;


/**
 * The IBANIdentifier atomic simple data type.
 * 
 * @author C24 Integration Objects;
 **/
public class IBANIdentifierClass extends biz.c24.io.api.data.GenericStringDataType 
{
    protected static IBANIdentifierClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected IBANIdentifierClass()
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
            synchronized (biz.c24.io.training.statements.IBANIdentifierClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.training.statements.IBANIdentifierClass();
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
        setName("IBANIdentifier");
        setModel(biz.c24.io.training.statements.StatementsDataModel.getInstance());
        setPatternType(biz.c24.io.api.data.PatternTypeEnum.SCHEMA);
        setPatternMatch("[a-zA-Z]{2,2}[0-9]{2,2}[a-zA-Z0-9]{1,30}");
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

}
