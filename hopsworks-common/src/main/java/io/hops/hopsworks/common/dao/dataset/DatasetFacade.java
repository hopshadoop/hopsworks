package io.hops.hopsworks.common.dao.dataset;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class DatasetFacade extends AbstractFacade<Dataset> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  private InodeFacade inodes;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public DatasetFacade() {
    super(Dataset.class);
  }

  @Override
  public List<Dataset> findAll() {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findAll",
            Dataset.class);
    return query.getResultList();
  }

  /**
   * Finds a dataset by id
   * <p/>
   * @param id
   * @return
   */
  public Dataset find(Integer id) {
    return em.find(Dataset.class, id);
  }

  /**
   * Finds all instances of a dataset. i.e if a dataset is shared it is going
   * to be present in the parent project and in the project it is shared with.
   * <p/>
   * @param inode
   * @return
   */
  public List<Dataset> findByInode(Inode inode) {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findByInode",
            Dataset.class).setParameter(
                    "inode", inode);
    return query.getResultList();
  }

  public Dataset findByNameAndProjectId(Project project, String name) {
    TypedQuery<Dataset> query = em.createNamedQuery(
            "Dataset.findByNameAndProjectId",
            Dataset.class);
    query.setParameter("name", name).setParameter("projectId", project);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Find by project and dataset name
   * <p/>
   * @param project
   * @param inode
   * @return
   */
  public Dataset findByProjectAndInode(Project project, Inode inode) {
    try {
      return em.createNamedQuery("Dataset.findByProjectAndInode", Dataset.class)
              .setParameter("projectId", project).setParameter(
              "inode", inode).getSingleResult();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Finds all data sets in a project.
   * <p/>
   * @param project
   * @return
   */
  public List<Dataset> findByProject(Project project) {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findByProject",
            Dataset.class).setParameter(
                    "projectId", project);
    return query.getResultList();
  }

  public List<DataSetDTO> findPublicDatasets() {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findAllPublic",
            Dataset.class);
    List<Dataset> datasets = query.getResultList();

    List<DataSetDTO> ds = new ArrayList<>();
    for (Dataset d : datasets) {
      DataSetDTO dto = new DataSetDTO();
      dto.setDescription(d.getDescription());
      dto.setName(d.getInode().getInodePK().getName());
      dto.setInodeId(d.getInode().getId());
      ds.add(dto);
    }
    return ds;
  }

  public void persistDataset(Dataset dataset) {
    em.persist(dataset);
  }

  public void flushEm() {
    em.flush();
  }

  public void merge(Dataset dataset) {
    em.merge(dataset);
    em.flush();
  }

  public void removeDataset(Dataset dataset) {
    Dataset ds = em.find(Dataset.class, dataset.getId());
    if (ds != null) {
      em.remove(ds);
    }
  }
}