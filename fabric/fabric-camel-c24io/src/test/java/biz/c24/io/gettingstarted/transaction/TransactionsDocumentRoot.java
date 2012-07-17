package biz.c24.io.gettingstarted.transaction;

import java.util.Arrays;

/**
 * Document Root. <p/>
 * This object is composed of the following <i>element</i>:
 * <ul>
 * <li><b>Transactions</b> of type {@link biz.c24.io.gettingstarted.transaction.Transactions} (1)</li>
 * </ul>
 * @author C24 Integration Objects;
 * @see biz.c24.io.gettingstarted.transaction.TransactionsDocumentRootElement.TransactionsDocumentRootClass
 **/
public class TransactionsDocumentRoot extends biz.c24.io.api.data.DocumentRoot 
{
    private static final java.lang.String[] NATIVE_ELEMENTS = new String[] {"Transactions"};
    private biz.c24.io.gettingstarted.transaction.Transactions transactions;

    /**
     * Constructs a new instance defined by the default element.
     **/
    public TransactionsDocumentRoot()
    {
        this(biz.c24.io.gettingstarted.transaction.TransactionsDocumentRootElement.TransactionsDocumentRootClass.getInstance().getNullDefiningElementDecl());
    }

    /**
     * Constructs a new instance defined by the specified element.
     * @param definingElementDecl The element which defines the object.
     **/
    public TransactionsDocumentRoot(biz.c24.io.api.data.Element definingElementDecl)
    {
        super(definingElementDecl);
    }

    /**
     * Constructs a new instance cloned from the specified object.
     * @param clone The object to be cloned.
     **/
    public TransactionsDocumentRoot(biz.c24.io.gettingstarted.transaction.TransactionsDocumentRoot clone)
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
            case 0:
                setTransactions((biz.c24.io.gettingstarted.transaction.Transactions) value);
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
        return new biz.c24.io.gettingstarted.transaction.TransactionsDocumentRoot(this);
    }

    /**
     * Creates and returns a deep clone of this object.
     * @return The new object.
     * @see #clone()
     **/
    public biz.c24.io.api.data.ComplexDataObject cloneDeep() throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.api.data.ComplexDataObject obj = new biz.c24.io.gettingstarted.transaction.TransactionsDocumentRoot(this);
        cloneDeep(obj);
        return obj;
    }

    protected void cloneDeep(biz.c24.io.api.data.ComplexDataObject clone) throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.gettingstarted.transaction.TransactionsDocumentRoot obj = (biz.c24.io.gettingstarted.transaction.TransactionsDocumentRoot) clone;
        obj.transactions = (biz.c24.io.gettingstarted.transaction.Transactions) biz.c24.io.api.Utils.cloneDeep(this.transactions, obj, "Transactions");
    }

    /**
     * Creates, adds and returns a new Transactions (1).
     * @return The new value.
     **/
    public biz.c24.io.gettingstarted.transaction.Transactions createTransactions()
    {
        biz.c24.io.gettingstarted.transaction.Transactions obj = (biz.c24.io.gettingstarted.transaction.Transactions) getElementDecl("Transactions").createObject();
        setTransactions(obj);
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
     * Returns the element called <code>name</code> at <code>index</code>.<p>
     * The legal value(s) for <code>name</code> are: <b>transactions</b>.
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
            case 0:
                return this.transactions;
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
            case 0:
                return this.transactions == null ? 0 : 1;
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
            case 0:
                return this.transactions != null && this.transactions.equals(element) ? 0 : -1;
            default:
                return super.getElementIndex(name, element);
        }
    }

    public int getTotalElementCount()
    {
        int count = 0;
        count += this.transactions == null ? 0 : 1;
        return count;
    }

    /**
     * Gets the value of Transactions (1).
     * @return The value.
     **/
    public biz.c24.io.gettingstarted.transaction.Transactions getTransactions()
    {
        return this.transactions;
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
            case 0:
                setTransactions(null);
                return;
            default:
            super.removeElement(name, index);
        }
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
            case 0:
                setTransactions((biz.c24.io.gettingstarted.transaction.Transactions) value);
                return;
            default:
                super.setElement(name, index, value);
        }
    }

    /**
     * Sets the value of Transactions (1).
     * @param value The new value.
     **/
    public void setTransactions(biz.c24.io.gettingstarted.transaction.Transactions value)
    {
        this.transactions = value;
        if (this.transactions != null)
            ((biz.c24.io.api.data.ComplexDataObject) this.transactions).setParent(this, "Transactions");
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException
    {
        out.writeObject(this.transactions);
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException
    {
        this.transactions = (biz.c24.io.gettingstarted.transaction.Transactions) in.readObject();
    }

}
