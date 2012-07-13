package biz.c24.io.training.statements;


/**
 * The Header complex data type.
 * 
 * @author C24 Integration Objects;
 * @see biz.c24.io.training.statements.Header
 **/
public class HeaderClass extends biz.c24.io.api.data.ComplexDataType 
{
    protected static HeaderClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected HeaderClass()
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
            synchronized (biz.c24.io.training.statements.HeaderClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.training.statements.HeaderClass();
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
        setName("Header");
        setModel(biz.c24.io.training.statements.StatementsDataModel.getInstance());
        setValidObjectClass(biz.c24.io.training.statements.Header.class);
        setContentModel(biz.c24.io.api.data.ContentModelEnum.SEQUENCE);
        biz.c24.io.api.data.Element element = null;
        addElementDecl(element = new biz.c24.io.api.data.Element("NameAddress", 1, 1, biz.c24.io.training.statements.PostalAddress1Class.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("StmtDate", 1, 1, biz.c24.io.training.statements.ISODateClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("StmtNo", 1, 1, biz.c24.io.training.statements.Max5IntegerClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("StmtPage", 1, 1, biz.c24.io.training.statements.Max5IntegerClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("Account", 1, 1, biz.c24.io.training.statements.IBANIdentifierClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("StartBalance", 1, 1, biz.c24.io.training.statements.CurrencyAndAmountClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

}
