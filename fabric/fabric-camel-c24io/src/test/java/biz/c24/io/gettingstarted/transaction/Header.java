package biz.c24.io.gettingstarted.transaction;

import java.util.Arrays;

/**
 * Header. <p/>
 * This object is composed of the following <i>elements</i>:
 * <ul>
 * <li><b>Name</b> of type {@link java.lang.String} (1)</li>
 * <li><b>Card Number</b> of type {@link java.lang.String} (1)</li>
 * <li><b>Expiry Date</b> of type {@link java.lang.String} (1)</li>
 * <li><b>Amount</b> of type {@link java.lang.String} (1)</li>
 * <li><b>Currency</b> of type {@link java.lang.String} (1)</li>
 * <li><b>Transaction Date</b> of type {@link java.lang.String} (1)</li>
 * <li><b>Commission</b> of type {@link java.lang.String} (1)</li>
 * <li><b>Vendor ID</b> of type {@link java.lang.String} (1)</li>
 * <li><b>Country</b> of type {@link java.lang.String} (1)</li>
 * </ul>
 * @author C24 Integration Objects;
 * @see biz.c24.io.gettingstarted.transaction.HeaderClass
 **/
public class Header extends biz.c24.io.api.data.ComplexDataObject 
{
    private static final java.lang.String[] NATIVE_ELEMENTS = new String[] {"Amount", "CardNumber", "Commission", "Country", "Currency", "ExpiryDate", "Name", "TransactionDate", "VendorID"};
    private java.lang.String name;
    private java.lang.String cardNumber;
    private java.lang.String expiryDate;
    private java.lang.String amount;
    private java.lang.String currency;
    private java.lang.String transactionDate;
    private java.lang.String commission;
    private java.lang.String vendorID;
    private java.lang.String country;

    /**
     * Constructs a new instance defined by the default element.
     **/
    public Header()
    {
        this(biz.c24.io.gettingstarted.transaction.HeaderClass.getInstance().getNullDefiningElementDecl());
    }

    /**
     * Constructs a new instance defined by the specified element.
     * @param definingElementDecl The element which defines the object.
     **/
    public Header(biz.c24.io.api.data.Element definingElementDecl)
    {
        super(definingElementDecl);
    }

    /**
     * Constructs a new instance defined by the specified element and type.
     * @param definingElementDecl The element which defines the object.
     * @param type The type which defines the object.
     **/
    public Header(biz.c24.io.api.data.Element definingElementDecl, biz.c24.io.api.data.ComplexDataType type)
    {
        super(definingElementDecl, type);
    }

    /**
     * Constructs a new instance cloned from the specified object.
     * @param clone The object to be cloned.
     **/
    public Header(biz.c24.io.gettingstarted.transaction.Header clone)
    {
        super(clone);
    }

    /**
     * Adds <code>value</code> as an element called <code>name</code>.<p>
     * The legal value(s) for <code>name</code> are defined in {@link #getElement}.
     **/
    public void addElement(java.lang.String name, java.lang.Object value)
    {
        name = makeSubstitution(name, -1);
        switch (Arrays.binarySearch(NATIVE_ELEMENTS, name))
        {
            case 6:
                setNameElement((java.lang.String) value);
                return;
            case 1:
                setCardNumber((java.lang.String) value);
                return;
            case 5:
                setExpiryDate((java.lang.String) value);
                return;
            case 0:
                setAmount((java.lang.String) value);
                return;
            case 4:
                setCurrency((java.lang.String) value);
                return;
            case 7:
                setTransactionDate((java.lang.String) value);
                return;
            case 2:
                setCommission((java.lang.String) value);
                return;
            case 8:
                setVendorID((java.lang.String) value);
                return;
            case 3:
                setCountry((java.lang.String) value);
                return;
            default:
                super.addElement(name, value);
        }
    }

    /**
     * Creates and returns a shallow clone of this object.
     * @see #cloneDeep()
     **/
    public java.lang.Object clone()
    {
        return new biz.c24.io.gettingstarted.transaction.Header(this);
    }

    /**
     * Creates and returns a deep clone of this object.
     * @return The new object.
     * @see #clone()
     **/
    public biz.c24.io.api.data.ComplexDataObject cloneDeep() throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.api.data.ComplexDataObject obj = new biz.c24.io.gettingstarted.transaction.Header(this);
        cloneDeep(obj);
        return obj;
    }

    protected void cloneDeep(biz.c24.io.api.data.ComplexDataObject clone) throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.gettingstarted.transaction.Header obj = (biz.c24.io.gettingstarted.transaction.Header) clone;
        obj.name = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.name, obj, "Name");
        obj.cardNumber = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.cardNumber, obj, "CardNumber");
        obj.expiryDate = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.expiryDate, obj, "ExpiryDate");
        obj.amount = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.amount, obj, "Amount");
        obj.currency = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.currency, obj, "Currency");
        obj.transactionDate = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.transactionDate, obj, "TransactionDate");
        obj.commission = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.commission, obj, "Commission");
        obj.vendorID = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.vendorID, obj, "VendorID");
        obj.country = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.country, obj, "Country");
    }

    public boolean equals(java.lang.Object obj)
    {
        if(obj instanceof biz.c24.io.api.data.ComplexDataObject)
            return equalContents((biz.c24.io.api.data.ComplexDataObject) obj, true, true, true, true);
        else
            return obj.equals(this);
    }

    /**
     * Gets the value of Amount (1).
     * @return The value.
     **/
    public java.lang.String getAmount()
    {
        return this.amount;
    }

    /**
     * Gets the value of CardNumber (1).
     * @return The value.
     **/
    public java.lang.String getCardNumber()
    {
        return this.cardNumber;
    }

    /**
     * Gets the value of Commission (1).
     * @return The value.
     **/
    public java.lang.String getCommission()
    {
        return this.commission;
    }

    /**
     * Gets the value of Country (1).
     * @return The value.
     **/
    public java.lang.String getCountry()
    {
        return this.country;
    }

    /**
     * Gets the value of Currency (1).
     * @return The value.
     **/
    public java.lang.String getCurrency()
    {
        return this.currency;
    }

    /**
     * Returns the element called <code>name</code> at <code>index</code>.<p>
     * The legal value(s) for <code>name</code> are: <b>name, cardNumber, expiryDate, amount, currency, transactionDate, commission, vendorID, country</b>.
     **/
    public java.lang.Object getElement(java.lang.String name, int index)
    {
        int i = Arrays.binarySearch(NATIVE_ELEMENTS, name);
        if (i < 0)
        {
            name = getSubstitute(name);
            i = Arrays.binarySearch(NATIVE_ELEMENTS, name);
        }
        switch (i)
        {
            case 6:
                return this.name;
            case 1:
                return this.cardNumber;
            case 5:
                return this.expiryDate;
            case 0:
                return this.amount;
            case 4:
                return this.currency;
            case 7:
                return this.transactionDate;
            case 2:
                return this.commission;
            case 8:
                return this.vendorID;
            case 3:
                return this.country;
            default:
                return super.getElement(name, index);
        }
    }

    /**
     * Returns the count of elements called <code>name</code>.<p>
     * The legal value(s) for <code>name</code> are defined in {@link #getElement}.
     **/
    public int getElementCount(java.lang.String name)
    {
        if (null == name) throw new NullPointerException(toString() + " was asked to calculate elements without name");
        int i = Arrays.binarySearch(NATIVE_ELEMENTS, name);
        if (i < 0)
        {
            name = getSubstitute(name);
            i = Arrays.binarySearch(NATIVE_ELEMENTS, name);
        }
        switch (i)
        {
            case 6:
                return this.name == null ? 0 : 1;
            case 1:
                return this.cardNumber == null ? 0 : 1;
            case 5:
                return this.expiryDate == null ? 0 : 1;
            case 0:
                return this.amount == null ? 0 : 1;
            case 4:
                return this.currency == null ? 0 : 1;
            case 7:
                return this.transactionDate == null ? 0 : 1;
            case 2:
                return this.commission == null ? 0 : 1;
            case 8:
                return this.vendorID == null ? 0 : 1;
            case 3:
                return this.country == null ? 0 : 1;
            default:
                return super.getElementCount(name);
        }
    }

    /**
     * Returns the element called <code>name</code> at <code>index</code>.<p>
     * The legal value(s) for <code>name</code> are defined in {@link #getElement}.
     **/
    public int getElementIndex(java.lang.String name, java.lang.Object element)
    {
        int i = Arrays.binarySearch(NATIVE_ELEMENTS, name);
        if (i < 0)
        {
            name = getSubstitute(name);
            i = Arrays.binarySearch(NATIVE_ELEMENTS, name);
        }
        switch (i)
        {
            case 6:
                return this.name != null && this.name.equals(element) ? 0 : -1;
            case 1:
                return this.cardNumber != null && this.cardNumber.equals(element) ? 0 : -1;
            case 5:
                return this.expiryDate != null && this.expiryDate.equals(element) ? 0 : -1;
            case 0:
                return this.amount != null && this.amount.equals(element) ? 0 : -1;
            case 4:
                return this.currency != null && this.currency.equals(element) ? 0 : -1;
            case 7:
                return this.transactionDate != null && this.transactionDate.equals(element) ? 0 : -1;
            case 2:
                return this.commission != null && this.commission.equals(element) ? 0 : -1;
            case 8:
                return this.vendorID != null && this.vendorID.equals(element) ? 0 : -1;
            case 3:
                return this.country != null && this.country.equals(element) ? 0 : -1;
            default:
                return super.getElementIndex(name, element);
        }
    }

    /**
     * Gets the value of ExpiryDate (1).
     * @return The value.
     **/
    public java.lang.String getExpiryDate()
    {
        return this.expiryDate;
    }

    /**
     * Gets the value of Name (1).
     * @return The value.
     **/
    public java.lang.String getNameElement()
    {
        return this.name;
    }

    public int getTotalElementCount()
    {
        int count = 0;
        count += this.name == null ? 0 : 1;
        count += this.cardNumber == null ? 0 : 1;
        count += this.expiryDate == null ? 0 : 1;
        count += this.amount == null ? 0 : 1;
        count += this.currency == null ? 0 : 1;
        count += this.transactionDate == null ? 0 : 1;
        count += this.commission == null ? 0 : 1;
        count += this.vendorID == null ? 0 : 1;
        count += this.country == null ? 0 : 1;
        return count;
    }

    /**
     * Gets the value of TransactionDate (1).
     * @return The value.
     **/
    public java.lang.String getTransactionDate()
    {
        return this.transactionDate;
    }

    /**
     * Gets the value of VendorID (1).
     * @return The value.
     **/
    public java.lang.String getVendorID()
    {
        return this.vendorID;
    }

    public int hashCode()
    {
        return this.toString().length();
    }

    /**
     * Removes the element called <code>name</code> at <code>index</code>.<p>
     * The legal value(s) for <code>name</code> are defined in {@link #getElement}.
     **/
    public void removeElement(java.lang.String name, int index)
    {
        name = unmakeSubstitution(name, index);
        switch (Arrays.binarySearch(NATIVE_ELEMENTS, name))
        {
            case 6:
                setNameElement(null);
                return;
            case 1:
                setCardNumber(null);
                return;
            case 5:
                setExpiryDate(null);
                return;
            case 0:
                setAmount(null);
                return;
            case 4:
                setCurrency(null);
                return;
            case 7:
                setTransactionDate(null);
                return;
            case 2:
                setCommission(null);
                return;
            case 8:
                setVendorID(null);
                return;
            case 3:
                setCountry(null);
                return;
            default:
            super.removeElement(name, index);
        }
    }

    /**
     * Sets the value of Amount (1).
     * @param value The new value.
     **/
    public void setAmount(java.lang.String value)
    {
        this.amount = value;
    }

    /**
     * Sets the value of CardNumber (1).
     * @param value The new value.
     **/
    public void setCardNumber(java.lang.String value)
    {
        this.cardNumber = value;
    }

    /**
     * Sets the value of Commission (1).
     * @param value The new value.
     **/
    public void setCommission(java.lang.String value)
    {
        this.commission = value;
    }

    /**
     * Sets the value of Country (1).
     * @param value The new value.
     **/
    public void setCountry(java.lang.String value)
    {
        this.country = value;
    }

    /**
     * Sets the value of Currency (1).
     * @param value The new value.
     **/
    public void setCurrency(java.lang.String value)
    {
        this.currency = value;
    }

    /**
     * Sets <code>value</code> as an element called <code>name</code> at <code>index</code>.<p>
     * The legal value(s) for <code>name</code> are defined in {@link #getElement}.
     **/
    public void setElement(java.lang.String name, int index, java.lang.Object value)
    {
        name = makeSubstitution(name, index);
        switch (Arrays.binarySearch(NATIVE_ELEMENTS, name))
        {
            case 6:
                setNameElement((java.lang.String) value);
                return;
            case 1:
                setCardNumber((java.lang.String) value);
                return;
            case 5:
                setExpiryDate((java.lang.String) value);
                return;
            case 0:
                setAmount((java.lang.String) value);
                return;
            case 4:
                setCurrency((java.lang.String) value);
                return;
            case 7:
                setTransactionDate((java.lang.String) value);
                return;
            case 2:
                setCommission((java.lang.String) value);
                return;
            case 8:
                setVendorID((java.lang.String) value);
                return;
            case 3:
                setCountry((java.lang.String) value);
                return;
            default:
                super.setElement(name, index, value);
        }
    }

    /**
     * Sets the value of ExpiryDate (1).
     * @param value The new value.
     **/
    public void setExpiryDate(java.lang.String value)
    {
        this.expiryDate = value;
    }

    /**
     * Sets the value of Name (1).
     * @param value The new value.
     **/
    public void setNameElement(java.lang.String value)
    {
        this.name = value;
    }

    /**
     * Sets the value of TransactionDate (1).
     * @param value The new value.
     **/
    public void setTransactionDate(java.lang.String value)
    {
        this.transactionDate = value;
    }

    /**
     * Sets the value of VendorID (1).
     * @param value The new value.
     **/
    public void setVendorID(java.lang.String value)
    {
        this.vendorID = value;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException
    {
        out.writeObject(this.name);
        out.writeObject(this.cardNumber);
        out.writeObject(this.expiryDate);
        out.writeObject(this.amount);
        out.writeObject(this.currency);
        out.writeObject(this.transactionDate);
        out.writeObject(this.commission);
        out.writeObject(this.vendorID);
        out.writeObject(this.country);
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException
    {
        this.name = (java.lang.String) in.readObject();
        this.cardNumber = (java.lang.String) in.readObject();
        this.expiryDate = (java.lang.String) in.readObject();
        this.amount = (java.lang.String) in.readObject();
        this.currency = (java.lang.String) in.readObject();
        this.transactionDate = (java.lang.String) in.readObject();
        this.commission = (java.lang.String) in.readObject();
        this.vendorID = (java.lang.String) in.readObject();
        this.country = (java.lang.String) in.readObject();
    }

}
