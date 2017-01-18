package se.kth.hopsworks.meta.wscomm;

import io.hops.common.Pair;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.hopsworks.dataset.Dataset;
import se.kth.hopsworks.dataset.DatasetFacade;
import se.kth.hopsworks.log.meta.MetaLog;
import se.kth.hopsworks.log.meta.MetaLogFacade;
import se.kth.hopsworks.log.ops.OperationType;
import se.kth.hopsworks.log.ops.OperationsLog;
import se.kth.hopsworks.log.ops.OperationsLogFacade;
import se.kth.hopsworks.meta.db.FieldFacade;
import se.kth.hopsworks.meta.db.FieldPredefinedValueFacade;
import se.kth.hopsworks.meta.db.MTableFacade;
import se.kth.hopsworks.meta.db.MetadataFacade;
import se.kth.hopsworks.meta.db.RawDataFacade;
import se.kth.hopsworks.meta.db.SchemalessMetadataFacade;
import se.kth.hopsworks.meta.db.TemplateFacade;
import se.kth.hopsworks.meta.db.TupleToFileFacade;
import se.kth.hopsworks.meta.entity.EntityIntf;
import se.kth.hopsworks.meta.entity.FieldPredefinedValue;
import se.kth.hopsworks.meta.entity.Field;
import se.kth.hopsworks.meta.entity.InodeTableComposite;
import se.kth.hopsworks.meta.entity.RawData;
import se.kth.hopsworks.meta.entity.MTable;
import se.kth.hopsworks.meta.entity.Metadata;
import se.kth.hopsworks.meta.entity.SchemalessMetadata;
import se.kth.hopsworks.meta.entity.Template;
import se.kth.hopsworks.meta.entity.TupleToFile;
import se.kth.hopsworks.meta.exception.ApplicationException;
import se.kth.hopsworks.meta.exception.DatabaseException;
import se.kth.hopsworks.util.HopsUtils;

/**
 *
 * @author Vangelis
 */
@Stateless(name = "metadataController")
public class MetadataController {

  private static final Logger logger = Logger.getLogger(MetadataController.class.getName());

  @EJB
  private TemplateFacade templateFacade;
  @EJB
  private MTableFacade tableFacade;
  @EJB
  private FieldFacade fieldFacade;
  @EJB
  private FieldPredefinedValueFacade fieldPredefinedValueFacade;
  @EJB
  private RawDataFacade rawDataFacade;
  @EJB
  private TupleToFileFacade tupletoFileFacade;
  @EJB
  private MetadataFacade metadataFacade;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private MetaLogFacade metaLogFacade;
  @EJB
  private OperationsLogFacade opsLogFacade;
  @EJB 
  private ProjectFacade projectFacade;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private SchemalessMetadataFacade schemalessMetadataFacade;
  
  public MetadataController() {
  }

  /**
   * Persist a new template in the database
   * <p/>
   * @param template
   * @return
   * @throws ApplicationException
   */
  public int addNewTemplate(Template template) throws ApplicationException {

    try {        
        if(!this.templateFacade.isTemplateAvailable(template.getName().toLowerCase())){
            return this.templateFacade.addTemplate(template);
        }
        else{
            throw new DatabaseException("Template name "+template.getName()+ " already available");
        }
            
      
    } catch (DatabaseException e) {
      throw new ApplicationException("Utils.java: Could not add new template "
              + template.getName(), e);
    }
  }

  /**
   * Updates a template name. addNewTemplate handles persisting/updating the
   * entity
   * <p/>
   * @param template
   * @return
   * @throws ApplicationException
   */
  public int updateTemplateName(Template template) throws ApplicationException {
    return this.addNewTemplate(template);
  }

  /**
   * Deletes a template from the database
   * <p/>
   * @param template
   * @throws ApplicationException
   */
  public void removeTemplate(Template template) throws ApplicationException {
    try {
      this.templateFacade.removeTemplate(template);
    } catch (DatabaseException e) {
      throw new ApplicationException("Utils.java: Could not remove template "
              + template.getName(), e);
    }
  }

  /**
   * Persist a list of tables and all their corresponding child entities in the
   * database
   * <p/>
   * @param list
   * @throws ApplicationException
   */
  public void addTables(List<EntityIntf> list) throws ApplicationException {

    for (EntityIntf entry : list) {
      MTable t = (MTable) entry;
      String tableName = t.getName();

      List<Field> tableFields = new LinkedList<>(t.getFields());
      t.resetFields();

      try {
        logger.log(Level.INFO, "STORE/UPDATE TABLE: {0} ", t);

        //persist the parent
        int tableId = this.tableFacade.addTable(t);
        for (Field field : tableFields) {
          //associate each field(child) with its table(parent)
          field.setTableid(tableId);

          List<EntityIntf> predef = new LinkedList<>(
                  (List<EntityIntf>) (List<?>) field.getFieldPredefinedValues());

          field.resetFieldPredefinedValues();
          //persist the child
          int fieldid = this.fieldFacade.addField(field);
          //remove any previous predefined values
          this.removeFieldPredefinedValues(fieldid);
          //add the new predefined values
          this.addFieldsPredefinedValues(predef, fieldid);
        }
      } catch (DatabaseException e) {
        throw new ApplicationException("Utils.java: Could not add table " + t.
                getName(), e);
      }
    }
  }

  /**
   * Stores the predefined values for a field
   * <p/>
   * @param list
   * @param fieldId
   * @throws ApplicationException
   */
  private void addFieldsPredefinedValues(List<EntityIntf> list, int fieldId)
          throws ApplicationException {

    try {
      for (EntityIntf entry : list) {
        FieldPredefinedValue predefval = (FieldPredefinedValue) entry;

        //associate each child with its parent
        predefval.setFieldid(fieldId);
        //persist the entity
        this.fieldPredefinedValueFacade.addFieldPredefinedValue(predefval);
      }
    } catch (DatabaseException e) {
      throw new ApplicationException(
              "Utils.java: Could not add predefined values", e);
    }
  }

  /**
   * Removes a table from the database
   * <p/>
   * @param table
   * @throws ApplicationException
   */
  public void deleteTable(MTable table) throws ApplicationException {
    try {
      logger.log(Level.INFO, "DELETING TABLE {0} ", table.getName());
      this.tableFacade.deleteTable(table);
    } catch (DatabaseException e) {
      throw new ApplicationException("Utils.java: could not delete table "
              + table);
    }
  }

  /**
   * Removes a field from the database
   * <p/>
   * @param field
   * @throws ApplicationException
   */
  public void deleteField(Field field) throws ApplicationException {

    try {
      logger.log(Level.INFO, "DELETING FIELD {0} ", field);
      this.fieldFacade.deleteField(field);
    } catch (DatabaseException e) {
      throw new ApplicationException("Utils.java: could not delete field "
              + field);
    }
  }

  /**
   * Removes a field's predefined values
   * <p/>
   * @param fieldid
   * @throws ApplicationException
   */
  public void removeFieldPredefinedValues(int fieldid) throws
          ApplicationException {
    try {
      this.fieldPredefinedValueFacade.deleteFieldPredefinedValues(fieldid);
    } catch (DatabaseException e) {
      throw new ApplicationException(
              "Utils.java: could not remove field predefined values ", e);
    }
  }

  /**
   * Stores the raw data for an inode. Creates a tuple first and
   * associates it with the given inode. Raw data at this point is just a field
   * id and a tupleid
   * <p/>
   * @param composite
   * @param raw
   * @throws ApplicationException
   */
  public void storeRawData(List<EntityIntf> composite, List<EntityIntf> raw)
          throws ApplicationException {

    try {
      /*
       * get the inodeid from the entity in the list. It is the
       * same for all the entities, since they constitute a single tuple
       */
      InodeTableComposite itc = (InodeTableComposite) composite.get(0);

      //get the inode
      Inode parent = this.inodeFacade.findById(itc.getInodePid());
      Inode inode = this.inodeFacade.findByInodePK(parent, itc.
              getInodeName(), HopsUtils.calculatePartitionId(parent.getId(), 
                      itc.getInodeName(), 3));

      //create a metadata tuple attached to be attached to an inodeid
      TupleToFile ttf = new TupleToFile(-1, inode);
      int tupleid = this.tupletoFileFacade.addTupleToFile(ttf);

      //every rawData entity carries the same inodeid
      for (EntityIntf raww : raw) {

        RawData r = (RawData) raww;
        r.getRawdataPK().setTupleid(tupleid);

        List<EntityIntf> metadataList
                = (List<EntityIntf>) (List<?>) new LinkedList<>(r.getMetadata());
        r.resetMetadata();

        //Persist the parent first
        //logger.log(Level.INFO, r.toString());
        this.rawDataFacade.addRawData(r);

        //move on to persist the child entities
        this.storeMetaData(metadataList, tupleid);
      }

    } catch (DatabaseException e) {
      throw new ApplicationException("Utils.java: could not store raw data ", e);
    }
  }

  /**
   * Updates a single raw data record.
   * <p/>
   * @param composite
   * @param metaId
   * @param metaObj
   * @throws ApplicationException
   */
  public void updateMetadata(List<EntityIntf> composite, int metaId, String metaObj) throws
          ApplicationException {
    try {
      Metadata metadata = this.metadataFacade.getMetadataById(metaId);
      metadata.setData(metaObj);
      this.metadataFacade.addMetadata(metadata);
      logMetadataOperation(metadata, OperationType.Update);
      
    } catch (DatabaseException e) {
      throw new ApplicationException("Utils.java: could not update metadata ", e);
    }
  }
  
  
    /**
   * Remove a single raw metadata record.
   * <p/>
   * @param composite
   * @param metaId
   * @param metaObj
   * @return 
   * @throws ApplicationException
   */
  public void removeMetadata(List<EntityIntf> composite, int metaId, String metaObj) throws
          ApplicationException {
    try {

      Metadata metadata = this.metadataFacade.getMetadataById(metaId);
      metadata.setData(metaObj);
      this.metadataFacade.removeMetadata(metadata);
      logMetadataOperation(metadata, OperationType.Delete);
      
    } catch (DatabaseException e) {
      throw new ApplicationException("Utils.java: could not delete metadata ", e);
    }
  }
  
  

  /**
   * Stores the actual metadata for an inode. Associates a raw data record with
   * a metadata record in the meta_data table
   * <p/>
   * @param metadatalist
   * @param tupleid
   * @throws ApplicationException
   */
  public void storeMetaData(List<EntityIntf> metadatalist, int tupleid) throws
          ApplicationException {

    try {
      for (EntityIntf entity : metadatalist) {
        Metadata metadata = (Metadata) entity;
        metadata.getMetadataPK().setTupleid(tupleid);

        //logger.log(Level.INFO, metadata.toString());
        this.metadataFacade.addMetadata(metadata);
        logMetadataOperation(metadata, OperationType.Add);
      }
    } catch (DatabaseException e) {
      throw new ApplicationException("Utils.java: could not store metadata ", e);
    }
  }
  
  public void addSchemaLessMetadata(String inodePath, String metadataJson) throws ApplicationException {
    Inode inode = inodeFacade.getInodeAtPath(inodePath);
    if (inode == null) {
      throw new ApplicationException("file " + inodePath + " doesn't exist");
    }

    boolean update = true;
    SchemalessMetadata metadata = schemalessMetadataFacade.findByInode(inode);
    if (metadata == null) {
      update = false;
      metadata = new SchemalessMetadata(inode);
    }
    metadata.setData(metadataJson);

    schemalessMetadataFacade.merge(metadata);
    logSchemaLessMetadataOperation(metadata, update ? OperationType.Update : OperationType.Add);
  }

  public void removeSchemaLessMetadata(String inodePath) throws ApplicationException {
    Inode inode = inodeFacade.getInodeAtPath(inodePath);
    if (inode == null) {
      throw new ApplicationException("file " + inodePath + " doesn't exist");
    }
    SchemalessMetadata metadata = schemalessMetadataFacade.findByInode(inode);
    if(metadata == null){
          throw new ApplicationException("No metadata attached with " + inodePath);
    }
    MetaLog removeOpLog = new MetaLog(metadata, OperationType.Delete);
    schemalessMetadataFacade.remove(metadata);
    metaLogFacade.persist(removeOpLog);
  }
    
  private void logSchemaLessMetadataOperation(SchemalessMetadata metadata, OperationType opType){
    metaLogFacade.persist(new MetaLog(metadata, opType));
  }
  
  private void logMetadataOperation(Metadata metadata, OperationType optype){
    metaLogFacade.persist(new MetaLog(metadata, optype));
  }
  
  public void logTemplateOperation(Template template, Inode inode, OperationType optype){
     Pair<Inode, Inode> project_dataset = inodeFacade.getProjectAndDatasetRootForInode(inode);
     Project project = projectFacade.findByInodeId(project_dataset.getL().getInodePK().getParentId(),
             project_dataset.getL().getInodePK().getName());
     Dataset dataset = datasetFacade.findByProjectAndInode(project, project_dataset.getR());
  
    opsLogFacade.persist(new OperationsLog(project, dataset, template, inode, optype));
  }
  
}