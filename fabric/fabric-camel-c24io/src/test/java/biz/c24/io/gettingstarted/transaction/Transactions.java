package biz.c24.io.gettingstarted.transaction;

import java.util.Arrays;

/**
 * Transactions. <p/>
 * This object is composed of the following <i>elements</i>:
 * <ul>
 * <li><b>Header</b> of type {@link biz.c24.io.gettingstarted.transaction.Header} (1)</li>
 * <li><b>Customer Details</b> of type {@link biz.c24.io.gettingstarted.transaction.CustomerDetails} (1..*)</li>
 * <li><b>Row Count</b> of type {@link biz.c24.io.gettingstarted.transaction.RowCount} (1)</li>
 * </ul>
 * @author C24 Integration Objects;
 * @see biz.c24.io.gettingstarted.transaction.TransactionsClass
 **/
public class Transactions extends biz.c24.io.api.data.ComplexDataObject 
{
    private static final java.lang.String[] NATIVE_ELEMENTS = new String[] {"CustomerDetails", "Header", "RowCount"};
    private biz.c24.io.gettingstarted.transaction.Header header;
    private biz.c24.io.gettingstarted.transaction.CustomerDetails[] customerDetails;
    private biz.c24.io.gettingstarted.transaction.RowCount rowCount;

    /**
     * Constructs a new instance defined by the default element.
     **/
    public Transactions()
    {
        this(biz.c24.io.gettingstarted.transaction.TransactionsClass.getInstance().getNullDefiningElementDecl());
    }

    /**
     * Constructs a new instance defined by the specified element.
     * @param definingElementDecl The element which defines the object.
     **/
    public Transactions(biz.c24.io.api.data.Element definingElementDecl)
    {
        super(definingElementDecl);
    }

    /**
     * Constructs a new instance defined by the specified element and type.
     * @param definingElementDecl The element which defines the object.
     * @param type The type which defines the object.
     **/
    public Transactions(biz.c24.io.api.data.Element definingElementDecl, biz.c24.io.api.data.ComplexDataType type)
    {
        super(definingElementDecl, type);
    }

    /**
     * Constructs a new instance cloned from the specified object.
     * @param clone The object to be cloned.
     **/
    public Transactions(biz.c24.io.gettingstarted.transaction.Transactions clone)
    {
        super(clone);
    }

    /**
     * Adds a CustomerDetails (1..*).
     * @param value The new CustomerDetails.
     **/
    public void addCustomerDetails(biz.c24.io.gettingstarted.transaction.CustomerDetails value)
    {
        biz.c24.io.gettingstarted.transaction.CustomerDetails[] temp = this.customerDetails;
        this.customerDetails = new biz.c24.io.gettingstarted.transaction.CustomerDetails[temp == null ? 1 : (temp.length+1)];
        if (temp != null)
            java.lang.System.arraycopy(temp, 0, this.customerDetails, 0, temp.length);
        this.customerDetails[this.customerDetails.length-1] = value;
        ((biz.c24.io.api.data.ComplexDataObject) value).setParent(this, "CustomerDetails");
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
            case 1:
                setHeader((biz.c24.io.gettingstarted.transaction.Header) value);
                return;
            case 0:
                addCustomerDetails((biz.c24.io.gettingstarted.transaction.CustomerDetails) value);
                return;
            case 2:
                setRowCount((biz.c24.io.gettingstarted.transaction.RowCount) value);
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
        return new biz.c24.io.gettingstarted.transaction.Transactions(this);
    }

    /**
     * Creates and returns a deep clone of this object.
     * @return The new object.
     * @see #clone()
     **/
    public biz.c24.io.api.data.ComplexDataObject cloneDeep() throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.api.data.ComplexDataObject obj = new biz.c24.io.gettingstarted.transaction.Transactions(this);
        cloneDeep(obj);
        return obj;
    }

    protected void cloneDeep(biz.c24.io.api.data.ComplexDataObject clone) throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.gettingstarted.transaction.Transactions obj = (biz.c24.io.gettingstarted.transaction.Transactions) clone;
        obj.header = (biz.c24.io.gettingstarted.transaction.Header) biz.c24.io.api.Utils.cloneDeep(this.header, obj, "Header");
        obj.customerDetails = (biz.c24.io.gettingstarted.transaction.CustomerDetails[]) biz.c24.io.api.Utils.cloneDeep(this.customerDetails, obj, "CustomerDetails");
        obj.rowCount = (biz.c24.io.gettingstarted.transaction.RowCount) biz.c24.io.api.Utils.cloneDeep(this.rowCount, obj, "RowCount");
    }

    /**
     * Creates, adds and returns a new CustomerDetails (1..*).
     * @return The new value.
     **/
    public biz.c24.io.gettingstarted.transaction.CustomerDetails createCustomerDetails()
    {
        biz.c24.io.gettingstarted.transaction.CustomerDetails obj = (biz.c24.io.gettingstarted.transaction.CustomerDetails) getElementDecl("CustomerDetails").createObject();
        addCustomerDetails(obj);
        return obj;
    }

    /**
     * Creates, adds and returns a new Header (1).
     * @return The new value.
     **/
    public biz.c24.io.gettingstarted.transaction.Header createHeader()
    {
        biz.c24.io.gettingstarted.transaction.Header obj = (biz.c24.io.gettingstarted.transaction.Header) getElementDecl("Header").createObject();
        setHeader(obj);
        return obj;
    }

    /**
     * Creates, adds and returns a new RowCount (1).
     * @return The new value.
     **/
    public biz.c24.io.gettingstarted.transaction.RowCount createRowCount()
    {
        biz.c24.io.gettingstarted.transaction.RowCount obj = (biz.c24.io.gettingstarted.transaction.RowCount) getElementDecl("RowCount").createObject();
        setRowCount(obj);
        return obj;
    }

    public boolean equals(java.lang.Object obj)
    {
        if(obj instanceof biz.c24.io.api.data.ComplexDataObject)
            return equalContents((biz.c24.io.api.data.ComplexDataObject) obj, true, true, true, true);
        else
            return obj.equals(this);
    }

    /**
     * Gets the value of CustomerDetails (1..*).
     * @return The value.
     **/
    public biz.c24.io.gettingstarted.transaction.CustomerDetails[] getCustomerDetails()
    {
        if (this.customerDetails == null)
            return new biz.c24.io.gettingstarted.transaction.CustomerDetails[]{};
        else
            return this.customerDetails;
    }

    /**
     * Gets the index of <code>value</code> (1..*).
     * @param value The CustomerDetails to get the index of.
     * @return The index.
     **/
    public int getCustomerDetailsIndex(biz.c24.io.gettingstarted.transaction.CustomerDetails value)
    {
        if (this.customerDetails == null)
            return -1;
        for (int i=0; i<this.customerDetails.length; i++)
            if (this.customerDetails[i] == value)
                return i;
        return -1;
    }

    /**
     * Returns the element called <code>name</code> at <code>index</code>.<p>
     * The legal value(s) for <code>name</code> are: <b>header, customerDetails, rowCount</b>.
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
            case 1:
                return this.header;
            case 0:
            if (this.customerDetails == null)
                throw new java.lang.ArrayIndexOutOfBoundsException();
            else
                return this.customerDetails[index];
            case 2:
                return this.rowCount;
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
            case 1:
                return this.header == null ? 0 : 1;
            case 0:
                return this.customerDetails == null ? 0 : this.customerDetails.length;
            case 2:
                return this.rowCount == null ? 0 : 1;
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
            case 1:
                return this.header != null && this.header.equals(element) ? 0 : -1;
            case 0:
                return getCustomerDetailsIndex((biz.c24.io.gettingstarted.transaction.CustomerDetails) element);
            case 2:
                return this.rowCount != null && this.rowCount.equals(element) ? 0 : -1;
            default:
                return super.getElementIndex(name, element);
        }
    }

    /**
     * Gets the value of Header (1).
     * @return The value.
     **/
    public biz.c24.io.gettingstarted.transaction.Header getHeader()
    {
        return this.header;
    }

    /**
     * Gets the value of RowCount (1).
     * @return The value.
     **/
    public biz.c24.io.gettingstarted.transaction.RowCount getRowCount()
    {
        return this.rowCount;
    }

    public int getTotalElementCount()
    {
        int count = 0;
        count += this.header == null ? 0 : 1;
        count += this.customerDetails == null ? 0 : this.customerDetails.length;
        count += this.rowCount == null ? 0 : 1;
        return count;
    }

    public int hashCode()
    {
        return this.toString().length();
    }

    /**
     * Removes a CustomerDetails (1..*).
     * @param index The index of the CustomerDetails to get.
     **/
    public void removeCustomerDetails(int index)
    {
        if (this.customerDetails == null)
            throw new java.lang.ArrayIndexOutOfBoundsException();
        biz.c24.io.gettingstarted.transaction.CustomerDetails[] temp = this.customerDetails;
        this.customerDetails = new biz.c24.io.gettingstarted.transaction.CustomerDetails[temp.length-1];
        java.lang.System.arraycopy(temp, 0, this.customerDetails, 0, index);
        java.lang.System.arraycopy(temp, index+1, this.customerDetails, index, temp.length-index-1);
        if (this.customerDetails.length == 0)
            this.customerDetails = null;
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
            case 1:
                setHeader(null);
                return;
            case 0:
                removeCustomerDetails(index);
                return;
            case 2:
                setRowCount(null);
                return;
            default:
            super.removeElement(name, index);
        }
    }

    /**
     * Sets the value of CustomerDetails (1..*).
     * @param value The new value.
     **/
    public void setCustomerDetails(biz.c24.io.gettingstarted.transaction.CustomerDetails[] value)
    {
        this.customerDetails = (biz.c24.io.gettingstarted.transaction.CustomerDetails[]) biz.c24.io.api.Utils.clearNulls(value);
        for (int i=0; this.customerDetails != null && i<this.customerDetails.length; i++)
            ((biz.c24.io.api.data.ComplexDataObject) this.customerDetails[i]).setParent(this, "CustomerDetails");
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
            case 1:
                setHeader((biz.c24.io.gettingstarted.transaction.Header) value);
                return;
            case 0:
                if (this.customerDetails == null)
                    throw new java.lang.ArrayIndexOutOfBoundsException();
                else if (value == null)
                    removeElement(name, index);
                else
                {
                    this.customerDetails[index] = (biz.c24.io.gettingstarted.transaction.CustomerDetails) value;
                    ((biz.c24.io.api.data.ComplexDataObject) this.customerDetails[index]).setParent(this, "CustomerDetails");
                }
                return;
            case 2:
                setRowCount((biz.c24.io.gettingstarted.transaction.RowCount) value);
                return;
            default:
                super.setElement(name, index, value);
        }
    }

    /**
     * Sets the value of Header (1).
     * @param value The new value.
     **/
    public void setHeader(biz.c24.io.gettingstarted.transaction.Header value)
    {
        this.header = value;
        if (this.header != null)
            ((biz.c24.io.api.data.ComplexDataObject) this.header).setParent(this, "Header");
    }

    /**
     * Sets the value of RowCount (1).
     * @param value The new value.
     **/
    public void setRowCount(biz.c24.io.gettingstarted.transaction.RowCount value)
    {
        this.rowCount = value;
        if (this.rowCount != null)
            ((biz.c24.io.api.data.ComplexDataObject) this.rowCount).setParent(this, "RowCount");
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException
    {
        out.writeObject(this.header);
        out.writeObject(this.customerDetails);
        out.writeObject(this.rowCount);
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException
    {
        this.header = (biz.c24.io.gettingstarted.transaction.Header) in.readObject();
        this.customerDetails = (biz.c24.io.gettingstarted.transaction.CustomerDetails[]) in.readObject();
        this.rowCount = (biz.c24.io.gettingstarted.transaction.RowCount) in.readObject();
    }

}
