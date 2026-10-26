package fixture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// One violation per rule, on a known line. Used by test/checks.test.js.
@Service
public class BadService {

    @Autowired
    private Repo repo;

    private String label = "a plain string";

    public String undocumented(String name) {
        return "{" + name;
    }

    /**
     * @param id the id
     */
    public String noDescription(Long id) {
        return em.createQuery("SELECT r FROM Retro r WHERE r.id = " + id).toString();
    }

    /**
     * Long on purpose, and the "{" in this comment must not count.
     *
     * @return nothing useful
     */
    public int tooLong() {
        int a = 1;
        a++;
        a++;
        a++;
        a++;
        a++;
        a++;
        a++;
        a++;
        a++;
        a++;
        a++;
        a++;
        a++;
        a++;
        a++;
        a++;
        a++;
        a++;
        a++;
        String brace = "{ not a block";
        return a;
    }
}
