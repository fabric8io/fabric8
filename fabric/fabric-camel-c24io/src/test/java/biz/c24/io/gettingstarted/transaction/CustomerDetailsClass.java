package biz.c24.io.gettingstarted.transaction;


/**
 * The Customer Details complex data type.
 * 
 * @author C24 Integration Objects;
 * @see biz.c24.io.gettingstarted.transaction.CustomerDetails
 **/
public class CustomerDetailsClass extends biz.c24.io.api.data.ComplexDataType 
{
    protected static CustomerDetailsClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected CustomerDetailsClass()
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
            synchronized (biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.gettingstarted.transaction.CustomerDetailsClass();
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
        setName("CustomerDetails");
        setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
        setDatabaseColumnName("Customer Details");
        setValidObjectClass(biz.c24.io.gettingstarted.transaction.CustomerDetails.class);
        setContentModel(biz.c24.io.api.data.ContentModelEnum.SEQUENCE);
        biz.c24.io.api.data.Element element = null;
        setTerminator(new String[] {new String(new char[] {0xd, 0xa})});
        setFormatType(biz.c24.io.api.data.FormatTypeEnum.DELIMITED);
        setDelimiter(new String[] {new String(new char[] {0x2c})});
        setDelimiterLocation(biz.c24.io.api.data.DelimiterLocationEnum.INFIX);
        setDelimiterPlaceholder(true);
        setDelimiterFieldWrapper(new String[] {new String(new char[] {0x22})});
        addElementDecl(element = new biz.c24.io.api.data.Element("Name", 1, 1, biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.NameClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("CardNumber", 1, 1, biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.CardNumberClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
        element.setDatabaseColumnName("Card Number");
        addElementDecl(element = new biz.c24.io.api.data.Element("ExpiryDate", 1, 1, biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.ExpiryDateClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
        element.setDatabaseColumnName("Expiry Date");
        addElementDecl(element = new biz.c24.io.api.data.Element("Amount", 1, 1, biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.AmountClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("Currency", 1, 1, biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.CurrencyClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("TransactionDate", 1, 1, biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.TransactionDateClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
        element.setDatabaseColumnName("Transaction Date");
        addElementDecl(element = new biz.c24.io.api.data.Element("Commission", 1, 1, biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.CommissionClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("VendorID", 1, 1, biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.VendorIDClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
        element.setDatabaseColumnName("Vendor ID");
        addElementDecl(element = new biz.c24.io.api.data.Element("Country", 1, 1, biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.CountryClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

    /**
     * The Amount atomic simple data type.
     * 
     * @author C24 Integration Objects;
     **/
    public static class AmountClass extends biz.c24.io.api.data.DoubleDataType 
    {
        protected static biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.AmountClass instance;
        private static boolean initialized;

        /**
         * Singleton constructor - use {@link #getInstance()} instead.
         **/
        protected AmountClass()
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
                synchronized (biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.AmountClass.class)
                {
                    if (instance == null)
                    {
                        instance = new biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.AmountClass();
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
            setName("Amount");
            setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
            setLocal(true);
            setAllFormatPatterns(new String[] {new String(new char[] {0x23, 0x30, 0x2e, 0x23, 0x3b, 0x27, 0x2d, 0x27, 0x23, 0x30, 0x2e, 0x23})});
            setDecimalSeparator('.');
        }

        private java.lang.Object readResolve()
        {
            return getInstance();
        }

    }


    /**
     * The Card Number atomic simple data type.
     * 
     * @author C24 Integration Objects;
     **/
    public static class CardNumberClass extends biz.c24.io.api.data.GenericStringDataType 
    {
        protected static biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.CardNumberClass instance;
        private static boolean initialized;

        /**
         * Singleton constructor - use {@link #getInstance()} instead.
         **/
        protected CardNumberClass()
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
                synchronized (biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.CardNumberClass.class)
                {
                    if (instance == null)
                    {
                        instance = new biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.CardNumberClass();
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
            setName("CardNumber");
            setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
            setDatabaseColumnName("Card Number");
            setLocal(true);
            setMinLength(0);
        }

        private java.lang.Object readResolve()
        {
            return getInstance();
        }

    }


    /**
     * The Commission atomic simple data type.
     * 
     * @author C24 Integration Objects;
     **/
    public static class CommissionClass extends biz.c24.io.api.data.DoubleDataType 
    {
        protected static biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.CommissionClass instance;
        private static boolean initialized;

        /**
         * Singleton constructor - use {@link #getInstance()} instead.
         **/
        protected CommissionClass()
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
                synchronized (biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.CommissionClass.class)
                {
                    if (instance == null)
                    {
                        instance = new biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.CommissionClass();
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
            setName("Commission");
            setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
            setLocal(true);
            setAllFormatPatterns(new String[] {new String(new char[] {0x23, 0x30, 0x2e, 0x23, 0x3b, 0x27, 0x2d, 0x27, 0x23, 0x30, 0x2e, 0x23})});
            setDecimalSeparator('.');
        }

        private java.lang.Object readResolve()
        {
            return getInstance();
        }

    }


    /**
     * The Country atomic simple data type.
     * 
     * @author C24 Integration Objects;
     **/
    public static class CountryClass extends biz.c24.io.api.data.GenericStringDataType 
    {
        protected static biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.CountryClass instance;
        private static boolean initialized;

        /**
         * Singleton constructor - use {@link #getInstance()} instead.
         **/
        protected CountryClass()
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
                synchronized (biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.CountryClass.class)
                {
                    if (instance == null)
                    {
                        instance = new biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.CountryClass();
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
            setName("Country");
            setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
            setLocal(true);
            setMinLength(0);
        }

        private java.lang.Object readResolve()
        {
            return getInstance();
        }

    }


    /**
     * The Currency atomic simple data type.
     * 
     * @author C24 Integration Objects;
     **/
    public static class CurrencyClass extends biz.c24.io.api.data.GenericStringDataType 
    {
        protected static biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.CurrencyClass instance;
        private static boolean initialized;

        /**
         * Singleton constructor - use {@link #getInstance()} instead.
         **/
        protected CurrencyClass()
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
                synchronized (biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.CurrencyClass.class)
                {
                    if (instance == null)
                    {
                        instance = new biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.CurrencyClass();
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
            setName("Currency");
            setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
            setLocal(true);
            setMinLength(0);
        }

        private java.lang.Object readResolve()
        {
            return getInstance();
        }

    }


    /**
     * The Expiry Date atomic simple data type.
     * 
     * @author C24 Integration Objects;
     **/
    public static class ExpiryDateClass extends biz.c24.io.api.data.GenericStringDataType 
    {
        protected static biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.ExpiryDateClass instance;
        private static boolean initialized;

        /**
         * Singleton constructor - use {@link #getInstance()} instead.
         **/
        protected ExpiryDateClass()
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
                synchronized (biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.ExpiryDateClass.class)
                {
                    if (instance == null)
                    {
                        instance = new biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.ExpiryDateClass();
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
            setName("ExpiryDate");
            setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
            setDatabaseColumnName("Expiry Date");
            setLocal(true);
            setMinLength(0);
        }

        private java.lang.Object readResolve()
        {
            return getInstance();
        }

    }


    /**
     * The Name atomic simple data type.
     * 
     * @author C24 Integration Objects;
     **/
    public static class NameClass extends biz.c24.io.api.data.GenericStringDataType 
    {
        protected static biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.NameClass instance;
        private static boolean initialized;

        /**
         * Singleton constructor - use {@link #getInstance()} instead.
         **/
        protected NameClass()
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
                synchronized (biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.NameClass.class)
                {
                    if (instance == null)
                    {
                        instance = new biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.NameClass();
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
            setName("Name");
            setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
            setLocal(true);
            setMinLength(0);
        }

        private java.lang.Object readResolve()
        {
            return getInstance();
        }

    }


    /**
     * The Transaction Date atomic simple data type.
     * 
     * @author C24 Integration Objects;
     **/
    public static class TransactionDateClass extends biz.c24.io.api.data.GenericDateDataType 
    {
        protected static biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.TransactionDateClass instance;
        private static boolean initialized;

        /**
         * Singleton constructor - use {@link #getInstance()} instead.
         **/
        protected TransactionDateClass()
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
                synchronized (biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.TransactionDateClass.class)
                {
                    if (instance == null)
                    {
                        instance = new biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.TransactionDateClass();
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
            setName("TransactionDate");
            setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
            setDatabaseColumnName("Transaction Date");
            setLocal(true);
            setAllFormatPatterns(new String[] {new String(new char[] {0x64, 0x64, 0x2d, 0x4d, 0x4d, 0x2d, 0x79, 0x79, 0x79, 0x79})});
        }

        private java.lang.Object readResolve()
        {
            return getInstance();
        }

    }


    /**
     * The Vendor ID atomic simple data type.
     * 
     * @author C24 Integration Objects;
     **/
    public static class VendorIDClass extends biz.c24.io.api.data.LongDataType 
    {
        protected static biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.VendorIDClass instance;
        private static boolean initialized;

        /**
         * Singleton constructor - use {@link #getInstance()} instead.
         **/
        protected VendorIDClass()
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
                synchronized (biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.VendorIDClass.class)
                {
                    if (instance == null)
                    {
                        instance = new biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.VendorIDClass();
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
            setName("VendorID");
            setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
            setDatabaseColumnName("Vendor ID");
            setLocal(true);
            setAllFormatPatterns(new String[] {new String(new char[] {0x23, 0x3b, 0x27, 0x2d, 0x27, 0x23})});
        }

        private java.lang.Object readResolve()
        {
            return getInstance();
        }

    }

}
