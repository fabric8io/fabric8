package biz.c24.io.training.statements;

import java.util.Arrays;

/**
 * Header. <p/>
 * This object is composed of the following <i>elements</i>:
 * <ul>
 * <li><b>NameAddress</b> of type {@link biz.c24.io.training.statements.PostalAddress1} (1)</li>
 * <li><b>StmtDate</b> of type {@link biz.c24.io.api.data.ISO8601Date} (1)</li>
 * <li><b>StmtNo</b> of type {@link java.math.BigInteger} (1)</li>
 * <li><b>StmtPage</b> of type {@link java.math.BigInteger} (1)</li>
 * <li><b>Account</b> of type {@link java.lang.String} (1)</li>
 * <li><b>StartBalance</b> of type {@link biz.c24.io.training.statements.CurrencyAndAmount} (1)</li>
 * </ul>
 * @author C24 Integration Objects;
 * @see biz.c24.io.training.statements.HeaderClass
 **/
public class Header extends biz.c24.io.api.data.ComplexDataObject 
{
    private static final java.lang.String[] NATIVE_ELEMENTS = new String[] {"Account", "NameAddress", "StartBalance", "StmtDate", "StmtNo", "StmtPage"};
    private biz.c24.io.training.statements.PostalAddress1 nameAddress;
    private biz.c24.io.api.data.ISO8601Date stmtDate;
    private java.math.BigInteger stmtNo;
    private java.math.BigInteger stmtPage;
    private java.lang.String account;
    private biz.c24.io.training.statements.CurrencyAndAmount startBalance;

    /**
     * Constructs a new instance defined by the default element.
     **/
    public Header()
    {
        this(biz.c24.io.training.statements.HeaderClass.getInstance().getNullDefiningElementDecl());
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
    public Header(biz.c24.io.training.statements.Header clone)
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
            case 1:
                setNameAddress((biz.c24.io.training.statements.PostalAddress1) value);
                return;
            case 3:
                setStmtDate((biz.c24.io.api.data.ISO8601Date) value);
                return;
            case 4:
                setStmtNo(biz.c24.io.api.Utils.integerValue(value));
                return;
            case 5:
                setStmtPage(biz.c24.io.api.Utils.integerValue(value));
                return;
            case 0:
                setAccount((java.lang.String) value);
                return;
            case 2:
                setStartBalance((biz.c24.io.training.statements.CurrencyAndAmount) value);
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
        return new biz.c24.io.training.statements.Header(this);
    }

    /**
     * Creates and returns a deep clone of this object.
     * @return The new object.
     * @see #clone()
     **/
    public biz.c24.io.api.data.ComplexDataObject cloneDeep() throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.api.data.ComplexDataObject obj = new biz.c24.io.training.statements.Header(this);
        cloneDeep(obj);
        return obj;
    }

    protected void cloneDeep(biz.c24.io.api.data.ComplexDataObject clone) throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.training.statements.Header obj = (biz.c24.io.training.statements.Header) clone;
        obj.nameAddress = (biz.c24.io.training.statements.PostalAddress1) biz.c24.io.api.Utils.cloneDeep(this.nameAddress, obj, "NameAddress");
        obj.stmtDate = (biz.c24.io.api.data.ISO8601Date) biz.c24.io.api.Utils.cloneDeep(this.stmtDate, obj, "StmtDate");
        obj.stmtNo = (java.math.BigInteger) biz.c24.io.api.Utils.cloneDeep(this.stmtNo, obj, "StmtNo");
        obj.stmtPage = (java.math.BigInteger) biz.c24.io.api.Utils.cloneDeep(this.stmtPage, obj, "StmtPage");
        obj.account = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.account, obj, "Account");
        obj.startBalance = (biz.c24.io.training.statements.CurrencyAndAmount) biz.c24.io.api.Utils.cloneDeep(this.startBalance, obj, "StartBalance");
    }

    /**
     * Creates, adds and returns a new NameAddress (1).
     * @return The new value.
     **/
    public biz.c24.io.training.statements.PostalAddress1 createNameAddress()
    {
        biz.c24.io.training.statements.PostalAddress1 obj = (biz.c24.io.training.statements.PostalAddress1) getElementDecl("NameAddress").createObject();
        setNameAddress(obj);
        return obj;
    }

    /**
     * Creates, adds and returns a new StartBalance (1).
     * @return The new value.
     **/
    public biz.c24.io.training.statements.CurrencyAndAmount createStartBalance()
    {
        biz.c24.io.training.statements.CurrencyAndAmount obj = (biz.c24.io.training.statements.CurrencyAndAmount) getElementDecl("StartBalance").createObject();
        setStartBalance(obj);
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
     * Gets the value of Account (1).
     * @return The value.
     **/
    public java.lang.String getAccount()
    {
        return this.account;
    }

    /**
     * Returns the element called <code>name</code> at <code>index</code>.<p>
     * The legal value(s) for <code>name</code> are: <b>nameAddress, stmtDate, stmtNo, stmtPage, account, startBalance</b>.
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
                return this.nameAddress;
            case 3:
                return this.stmtDate;
            case 4:
                return this.stmtNo;
            case 5:
                return this.stmtPage;
            case 0:
                return this.account;
            case 2:
                return this.startBalance;
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
                return this.nameAddress == null ? 0 : 1;
            case 3:
                return this.stmtDate == null ? 0 : 1;
            case 4:
                return this.stmtNo == null ? 0 : 1;
            case 5:
                return this.stmtPage == null ? 0 : 1;
            case 0:
                return this.account == null ? 0 : 1;
            case 2:
                return this.startBalance == null ? 0 : 1;
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
                return this.nameAddress != null && this.nameAddress.equals(element) ? 0 : -1;
            case 3:
                return this.stmtDate != null && this.stmtDate.equals(element) ? 0 : -1;
            case 4:
                return this.stmtNo != null && this.stmtNo.equals(element) ? 0 : -1;
            case 5:
                return this.stmtPage != null && this.stmtPage.equals(element) ? 0 : -1;
            case 0:
                return this.account != null && this.account.equals(element) ? 0 : -1;
            case 2:
                return this.startBalance != null && this.startBalance.equals(element) ? 0 : -1;
            default:
                return super.getElementIndex(name, element);
        }
    }

    /**
     * Gets the value of NameAddress (1).
     * @return The value.
     **/
    public biz.c24.io.training.statements.PostalAddress1 getNameAddress()
    {
        return this.nameAddress;
    }

    /**
     * Gets the value of StartBalance (1).
     * @return The value.
     **/
    public biz.c24.io.training.statements.CurrencyAndAmount getStartBalance()
    {
        return this.startBalance;
    }

    /**
     * Gets the value of StmtDate (1).
     * @return The value.
     **/
    public biz.c24.io.api.data.ISO8601Date getStmtDate()
    {
        return this.stmtDate;
    }

    /**
     * Gets the value of StmtNo (1).
     * @return The value.
     **/
    public java.math.BigInteger getStmtNo()
    {
        return this.stmtNo;
    }

    /**
     * Gets the value of StmtPage (1).
     * @return The value.
     **/
    public java.math.BigInteger getStmtPage()
    {
        return this.stmtPage;
    }

    public int getTotalElementCount()
    {
        int count = 0;
        count += this.nameAddress == null ? 0 : 1;
        count += this.stmtDate == null ? 0 : 1;
        count += this.stmtNo == null ? 0 : 1;
        count += this.stmtPage == null ? 0 : 1;
        count += this.account == null ? 0 : 1;
        count += this.startBalance == null ? 0 : 1;
        return count;
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
            case 1:
                setNameAddress(null);
                return;
            case 3:
                setStmtDate(null);
                return;
            case 4:
                setStmtNo(null);
                return;
            case 5:
                setStmtPage(null);
                return;
            case 0:
                setAccount(null);
                return;
            case 2:
                setStartBalance(null);
                return;
            default:
            super.removeElement(name, index);
        }
    }

    /**
     * Sets the value of Account (1).
     * @param value The new value.
     **/
    public void setAccount(java.lang.String value)
    {
        this.account = value;
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
                setNameAddress((biz.c24.io.training.statements.PostalAddress1) value);
                return;
            case 3:
                setStmtDate((biz.c24.io.api.data.ISO8601Date) value);
                return;
            case 4:
                setStmtNo(biz.c24.io.api.Utils.integerValue(value));
                return;
            case 5:
                setStmtPage(biz.c24.io.api.Utils.integerValue(value));
                return;
            case 0:
                setAccount((java.lang.String) value);
                return;
            case 2:
                setStartBalance((biz.c24.io.training.statements.CurrencyAndAmount) value);
                return;
            default:
                super.setElement(name, index, value);
        }
    }

    /**
     * Sets the value of NameAddress (1).
     * @param value The new value.
     **/
    public void setNameAddress(biz.c24.io.training.statements.PostalAddress1 value)
    {
        this.nameAddress = value;
        if (this.nameAddress != null)
            ((biz.c24.io.api.data.ComplexDataObject) this.nameAddress).setParent(this, "NameAddress");
    }

    /**
     * Sets the value of StartBalance (1).
     * @param value The new value.
     **/
    public void setStartBalance(biz.c24.io.training.statements.CurrencyAndAmount value)
    {
        this.startBalance = value;
        if (this.startBalance != null)
            ((biz.c24.io.api.data.ComplexDataObject) this.startBalance).setParent(this, "StartBalance");
    }

    /**
     * Sets the value of StmtDate (1).
     * @param value The new value.
     **/
    public void setStmtDate(biz.c24.io.api.data.ISO8601Date value)
    {
        this.stmtDate = value;
    }

    /**
     * Sets the value of StmtNo (1).
     * @param value The new value.
     **/
    public void setStmtNo(java.math.BigInteger value)
    {
        this.stmtNo = value;
    }

    /**
     * Sets the value of StmtPage (1).
     * @param value The new value.
     **/
    public void setStmtPage(java.math.BigInteger value)
    {
        this.stmtPage = value;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException
    {
        out.writeObject(this.nameAddress);
        out.writeObject(this.stmtDate);
        out.writeObject(this.stmtNo);
        out.writeObject(this.stmtPage);
        out.writeObject(this.account);
        out.writeObject(this.startBalance);
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException
    {
        this.nameAddress = (biz.c24.io.training.statements.PostalAddress1) in.readObject();
        this.stmtDate = (biz.c24.io.api.data.ISO8601Date) in.readObject();
        this.stmtNo = (java.math.BigInteger) in.readObject();
        this.stmtPage = (java.math.BigInteger) in.readObject();
        this.account = (java.lang.String) in.readObject();
        this.startBalance = (biz.c24.io.training.statements.CurrencyAndAmount) in.readObject();
    }

}
