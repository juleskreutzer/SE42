package util;

/**
 * Created by juleskreutzer on 09-05-16.
 */

import static org.junit.Assert.*;

import bank.dao.AccountDAOJPAImpl;
import bank.domain.Account;
import org.junit.After;
import org.junit.Before;
import org.junit.Test
        ;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.sql.SQLException;
import java.util.List;

public class Assignment1 {

    private EntityManagerFactory emf = Persistence.createEntityManagerFactory("SE42");
    private EntityManager em;

    public Assignment1() {
    }

    @Before
    public void setUp() {
        em = emf.createEntityManager();
    }

    @After
    public void tearDown() {
        try{
            new DatabaseCleaner(em).clean();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void persistCommitTest() {
        Account account = new Account(112L);
        em.getTransaction().begin();
        em.persist(account);
        /**
         * Instance will be created, but result will NOT be written to database
         */
        assertNull(account.getId());
        em.getTransaction().commit();
        System.out.println("AccountId: " + account.getId());
        /**
         * By calling the commit function, all data will be written to the database.
         * If an error occurs, for example Duplicate Key, it will be thrown at this point
         */
        assertTrue(account.getId() > 0L);
    }

    @Test
    public void rollbackTest() {
        Account account = new Account(111L);
        em.getTransaction().begin();
        em.persist(account);
        assertNull(account.getId());
        em.getTransaction().rollback();
        AccountDAOJPAImpl accountDAOJPAImpl = new AccountDAOJPAImpl(em);
        List<Account> findAll = accountDAOJPAImpl.findAll();
        assertEquals(findAll.size(), 0);
        /**
         * The function accountDAOJPAImpl.loadAll() loads all data from the database. When no data is stored, the list is empty and the size will be 0.
         */
    }

    @Test
    public void flushTest() {
        Long expected = -100L;
        Account account = new Account(111L);
        account.setId(expected);
        em.getTransaction().begin();
        em.persist(account);
        /**
         * The data hasn't been committed to the database, so the account doesn't have an ID. Thats why ID equals -100L.
         */
        assertEquals(expected, account.getId());
        em.flush();
        /**
         * Flush checks if the data is correct with the data in the database, but doesn't commit it.
         * To commit the data to the database, call the commit function.
         */
        assertNotEquals(expected, account.getId());
        em.getTransaction().commit();
        /**
         * Data is stored in database now...
         */
    }

    @Test
    public void editAfterPersistTest() {
        Long expectedBalance = 400L;
        Account account = new Account(114L);
        em.getTransaction().begin();
        em.persist(account);
        account.setBalance(expectedBalance);
        em.getTransaction().commit();
        assertEquals(expectedBalance, account.getBalance());
        /**
         * account.getBalance() is the same as expectedBalance. This means that the commit() method will save the data to the database that were given to the em when persist()
         * was called, or that that the data that was changed after the persist method are automatically stored in the database.
         * More explained below...
         */
        Long acId = account.getId();
        account = null;
        EntityManager em2 = emf.createEntityManager();
        em2.getTransaction().begin();
        Account found = em2.find(Account.class, acId);
        assertEquals(expectedBalance, found.getBalance());
        /**
         * found.getBAlance equals expectedBalance. This means that the changes that are done after the persist() method are also stored in the database when commit() is called.
         */
    }

    @Test
    public void refreshTest() {
        Long expectedBalance = 400L;
        Long changedBalance = 200L;
        Account account = new Account(114L);
        em.getTransaction().begin();
        em.persist(account);
        account.setBalance(expectedBalance);
        em.getTransaction().commit();
        assertEquals(expectedBalance, account.getBalance());
        Long acId = account.getId();
        EntityManager em2 = emf.createEntityManager();
        em2.getTransaction().begin();
        Account found = em2.find(Account.class, acId);
        assertEquals(expectedBalance, found.getBalance());

        account.setBalance(changedBalance);
        em.getTransaction().begin();
        em.getTransaction().commit();
        em2.refresh(found);
        assertEquals(changedBalance, found.getBalance());
        /**
         * When commit() is called, the changed account data is stored in the database. When refresh() is called,
         * the data will be updated to match the data in the database
         */
    }

    @Test
    public void MergeTest() {
        Account acc = new Account(1L);
        Account acc2;

        // scenario 1
        Long balance1 = 100L;
        em.getTransaction().begin();
        em.persist(acc);
        acc.setBalance(balance1);
        em.getTransaction().commit();
        assertEquals(balance1, acc.getBalance());
        /**
         * Er is hier nog maar een account waardoor er dus ook maar een assert
         * nodig is.
         */

        // scenario 2
        Long balance2a = 211L;
        Long balance2b = 222L;
        em.getTransaction().begin();
        acc2 = em.merge(acc);
        acc.setBalance(balance2a);
        acc2.setBalance(balance2b);
        em.getTransaction().commit();
        assertEquals(balance2b, acc.getBalance());
        assertEquals(balance2b, acc2.getBalance());
        /**
         * The balance of acc is changed with balance2a and the balance of acc2 is changed with balance2b
         * bacause acc2.setBalance() is called after merge(), the merge result will be overwritten.
         */

        // scenario 3
        Long balance3c = 333L;
        Long balance3d = 344L;
        em.getTransaction().begin();
        acc2 = em.merge(acc);
        assertTrue(em.contains(acc));
        assertTrue(em.contains(acc2));
        assertEquals(acc, acc2);
        acc2.setBalance(balance3c);
        acc.setBalance(balance3d);
        em.getTransaction().commit();
        assertEquals(balance3d, acc.getBalance());
        assertEquals(balance3d, acc2.getBalance());
        /**
         * What is the difference with scenario 2?
         */

        // scenario 4
        Account account = new Account(114L);
        account.setBalance(450L);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(account);
        em.getTransaction().commit();

        Account account2 = new Account(114L);
        Account tweedeAccountObject = account2;
        tweedeAccountObject.setBalance(650l);
        assertEquals((Long) 650L, account2.getBalance());

        account2.setId(account.getId());
        em.getTransaction().begin();
        account2 = em.merge(account2);
        assertSame(account, account2);
        assertTrue(em.contains(account2));
        assertFalse(em.contains(tweedeAccountObject));
        tweedeAccountObject.setBalance(850l);
        assertEquals((Long) 650L, account.getBalance());
        assertEquals((Long) 650L, account2.getBalance());
        em.getTransaction().commit();
        em.close();
    }

    @Test
    public void findAndClearTest() {
        Account acc1 = new Account(77L);
        em.getTransaction().begin();
        em.persist(acc1);
        em.getTransaction().commit();
        //Database bevat nu een account.

        // scenario 1
        Account accF1;
        Account accF2;
        accF1 = em.find(Account.class, acc1.getId());
        accF2 = em.find(Account.class, acc1.getId());
        assertSame(accF1, accF2);

        // scenario 2
        accF1 = em.find(Account.class, acc1.getId());
        em.clear();
        accF2 = em.find(Account.class, acc1.getId());
        assertNotSame(accF1, accF2);
        /**
         * clear() removes the local data, so the data of accF1 is not equal to accF2.
         * Also, changes to accF1 will not have any effect.
         */
    }

    @Test
    public void removeTest() {
        Account acc1 = new Account(88L);
        em.getTransaction().begin();
        em.persist(acc1);
        em.getTransaction().commit();
        Long id = acc1.getId();
        //Database bevat nu een account.

        em.remove(acc1);
        assertEquals(id, acc1.getId());
        Account accFound = em.find(Account.class, id);
        assertNull(accFound);
        /**
         * First check if we have correct ID because it's needed to check if accFound is null.
         * Then we remove acc1 form EntityManager. This results in null when we call assertNull(accFound).
         */
    }

    /**
     * For generation type, adjustments have been made in Account.class
     * SEQUENCE: test where the same
     * TABLE: All tests failed
     */


}
