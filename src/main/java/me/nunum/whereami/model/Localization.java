package me.nunum.whereami.model;

import me.nunum.whereami.framework.domain.Identifiable;
import me.nunum.whereami.framework.dto.DTO;
import me.nunum.whereami.framework.dto.DTOable;
import me.nunum.whereami.model.dto.LocalizationDTO;
import org.eclipse.persistence.annotations.Index;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"label", "user"}))
@NamedQueries({
        @NamedQuery(
                name = "Localization.allVisibleLocalizations",
                query = "SELECT OBJECT (l) FROM Localization l WHERE l.isPublic=true OR l.owner.id=:ownerId ORDER BY l.id DESC"
        ),
        @NamedQuery(
                name = "Localization.allVisibleLocalizationsFilterByName",
                query = "SELECT OBJECT (l) FROM Localization l WHERE (l.isPublic=true OR l.owner.id=:ownerId) AND l.label LIKE :name ORDER BY l.id DESC"
        ),
        @NamedQuery(
                name = "Localization.allVisibleLocalizationsFilterByTraining",
                query = "SELECT OBJECT (l) FROM Localization l JOIN l.trainings t WHERE (l.isPublic=true OR l.owner.id=:ownerId) HAVING t.status=3 ORDER BY l.id DESC"
        )
})
public class Localization implements DTOable, Identifiable<Long>, Comparable<Localization> {

    @Id
    @GeneratedValue
    private Long id;

    @Column(length = 100)
    private String label;

    private Long samples;

    private Float accuracy;

    private Integer numberOfPositions;

    private Double latitude;

    private Double longitude;

    @Index
    private boolean isPublic;

    @Column(length = 100)
    private String user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Device owner;

    @OneToMany(orphanRemoval = true, cascade = CascadeType.REMOVE, mappedBy = "localization")
    private List<Position> positionList;

    @OneToMany(orphanRemoval = true, cascade = CascadeType.REMOVE, mappedBy = "localization")
    private List<Training> trainings;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "SPAM_LOCALIZATION_ID")
    private LocalizationSpamReport spamReport;

    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updated;

    protected Localization() {
        //JPA
    }


    public Localization(String label,
                        String userLabel, Device owner) {
        this(label, userLabel, 0.0, 0.0, false, owner);
    }


    public Localization(String label,
                        String userLabel,
                        Double latitude,
                        Double longitude,
                        boolean isPublic,
                        Device owner) {

        this.label = label;
        this.latitude = latitude;
        this.longitude = longitude;
        this.samples = 0L;
        this.accuracy = 0.0f;
        this.numberOfPositions = 0;
        this.user = userLabel;
        this.owner = owner;
        this.isPublic = isPublic;
        this.trainings = new ArrayList<>();
        this.positionList = new ArrayList<>();
        this.spamReport = new LocalizationSpamReport(this);
    }


    @PrePersist
    protected void onCreate() {
        updated = created = new Date(System.currentTimeMillis());
    }

    @PreUpdate
    protected void onUpdate() {
        updated = new Date(System.currentTimeMillis());
    }

    @Override
    public DTO toDTO() {
        return new LocalizationDTO(this.id,
                this.label,
                this.user,
                this.samples,
                this.accuracy,
                this.numberOfPositions,
                false
        );
    }

    public DTO toDTO(Device requester) {
        return new LocalizationDTO(this.id,
                this.label,
                this.user,
                this.samples,
                this.accuracy,
                this.numberOfPositions,
                this.owner.equals(requester)
        );
    }

    @Override
    public boolean is(Long id) {
        return this.id.equals(id);
    }

    @Override
    public Long id() {
        return this.id;
    }

    public boolean isOwner(Device requester) {
        return this.owner.equals(requester);
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void incrementSample() {
        this.samples += 1;
    }

    public void incrementPosition() {
        this.numberOfPositions += 1;
    }

    public void decrementPosition() {
        this.numberOfPositions -= 1;
    }

    public boolean addTraining(Training training) {

        this.trainings.add(training);
        if (!Objects.equals(training.getLocalization().id(), this.id)) {
            training.setLocalization(this);
        }

        return true;
    }

    public Device getOwner() {
        return owner;
    }

    public LocalizationSpamReport getSpamReport() {
        return spamReport;
    }

    public void addSpamReporter(Device reporter) {
        this.spamReport.newReport(reporter);
    }

    public List<Training> getTrainings() {
        return trainings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Localization that = (Localization) o;
        return Objects.equals(label, that.label) &&
                Objects.equals(owner, that.owner);
    }

    @Override
    public int hashCode() {

        return Objects.hash(label, owner);
    }

    @Override
    public int compareTo(Localization o) {
        return this.label.compareToIgnoreCase(o.label);
    }

    public String positionLabelById(Long positionPredicated) {

        final Optional<Position> optionalPosition = this.positionList.stream().filter(e -> e.id().equals(positionPredicated)).findFirst();

        return optionalPosition.isPresent() ? optionalPosition.get().getLabel() : "";
    }
}
