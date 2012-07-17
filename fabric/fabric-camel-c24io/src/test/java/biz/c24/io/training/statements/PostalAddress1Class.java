package biz.c24.io.training.statements;


/**
 * The PostalAddress1 complex data type.
 * 
 * @author C24 Integration Objects;
 * @see biz.c24.io.training.statements.PostalAddress1
 **/
public class PostalAddress1Class extends biz.c24.io.api.data.ComplexDataType 
{
    protected static PostalAddress1Class instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected PostalAddress1Class()
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
            synchronized (biz.c24.io.training.statements.PostalAddress1Class.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.training.statements.PostalAddress1Class();
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
        setName("PostalAddress1");
        setModel(biz.c24.io.training.statements.StatementsDataModel.getInstance());
        setValidObjectClass(biz.c24.io.training.statements.PostalAddress1.class);
        setContentModel(biz.c24.io.api.data.ContentModelEnum.SEQUENCE);
        biz.c24.io.api.data.Element element = null;
        addElementDecl(element = new biz.c24.io.api.data.Element("AdrTp", 0, 1, biz.c24.io.training.statements.AddressType2CodeClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("AdrLine", 0, 5, biz.c24.io.training.statements.Max70TextClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("StrtNm", 0, 1, biz.c24.io.training.statements.Max70TextClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("BldgNb", 0, 1, biz.c24.io.training.statements.Max16TextClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("PstCd", 0, 1, biz.c24.io.training.statements.Max16TextClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("TwnNm", 0, 1, biz.c24.io.training.statements.Max35TextClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("CtrySubDvsn", 0, 1, biz.c24.io.training.statements.Max35TextClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("Ctry", 1, 1, biz.c24.io.training.statements.CountryCodeClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

}
