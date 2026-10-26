package fixture;

import org.springframework.stereotype.Service;

@Service
public class GoodService {

    private final Repo repo;

    @Autowired
    public GoodService(Repo repo) {
        this.repo = repo;
    }

    /**
     * Finds a retro by id.
     *
     * @param id the id
     * @return the title
     */
    @Transactional
    public String find(Long id) {
        String json = """
                { "unbalanced": "{{{" }
                """;
        return repo.query("SELECT r FROM Retro r WHERE r.id = :id", id) + json;
    }
}
