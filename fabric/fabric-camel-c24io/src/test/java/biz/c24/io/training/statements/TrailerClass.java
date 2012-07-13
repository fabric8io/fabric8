package biz.c24.io.training.statements;


/**
 * The Trailer complex data type.
 * 
 * @author C24 Integration Objects;
 * @see biz.c24.io.training.statements.Trailer
 **/
public class TrailerClass extends biz.c24.io.api.data.ComplexDataType 
{
    protected static TrailerClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected TrailerClass()
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
            synchronized (biz.c24.io.training.statements.TrailerClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.training.statements.TrailerClass();
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
        setName("Trailer");
        setModel(biz.c24.io.training.statements.StatementsDataModel.getInstance());
        setValidObjectClass(biz.c24.io.training.statements.Trailer.class);
        setContentModel(biz.c24.io.api.data.ContentModelEnum.SEQUENCE);
        biz.c24.io.api.data.Element element = null;
        addElementDecl(element = new biz.c24.io.api.data.Element("EndBalance", 1, 1, biz.c24.io.training.statements.CurrencyAndAmountClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

}
