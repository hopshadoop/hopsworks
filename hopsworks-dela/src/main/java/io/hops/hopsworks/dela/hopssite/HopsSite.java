package io.hops.hopsworks.dela.hopssite;

import io.hops.hopsworks.dela.exception.ThirdPartyException;

public class HopsSite {

  public static class ClusterService {

    public static String delaVersion() {
      return "public/cluster/dela/version";
    }

    public static String registerCluster() {
      return "private/cluster/register";
    }

    public static String heavyPing(String publicCId) {
      return "private/cluster/heavyPing/" + publicCId;
    }

    public static String ping(String publicCId) {
      return "private/cluster/ping/" + publicCId;
    }
  }

  public static class UserService {
    public static String user() {
      return "private/user";
    }
    
    public static String registerUser(String publicCId) {
      return "private/user/register/" + publicCId;
    }

    public static String getUser(String publicCId, String email) {
      return "private/user/" + publicCId + "/" + email;
    }
  }

  public static class DatasetService {

    public static String dataset() {
      return "private/dataset";
    }
    
    public static String datasetByPublicId() {
      return "private/dataset/byPublicId";
    }
    
    public static String datasetIssue() {
      return "private/dataset/issue";
    }
    
    public static String datasetCategory() {
      return "private/dataset/category";
    }
    
    public static String datasetPopular() {
      return "private/dataset/popular";
    }
    
    public static String publish(String publicCId) {
      return "private/dataset/publish/" + publicCId;
    }

    public static String unpublish(String publicCId, String publicDSId) {
      return "private/dataset/unpublish/" + publicCId + "/" + publicDSId;
    }

    public static String download(String publicCId, String publicDSId) {
      return "private/dataset/download/" + publicCId + "/" + publicDSId;
    }

    public static String complete(String publicCId, String publicDSId) {
      return "private/dataset/complete/" + publicCId + "/" + publicDSId;
    }

    public static String remove(String publicCId, String publicDSId) {
      return "private/dataset/remove/" + publicCId + "/" + publicDSId;
    }

    public static String search() {
      return "public/dataset/search";
    }

    public static String searchPage(String sessionId, Integer startItem, Integer nrItems) {
      return "public/dataset/search/" + sessionId + "/page/" + startItem + "/" + nrItems;
    }

    public static String details(String publicDSId) {
      return "public/dataset/" + publicDSId + "/details";
    }
  }

  public static class RatingService {
    
    public static String rating() {
      return "private/rating";
    }
    
    public static String getDatasetAllByPublicId() {
      return "private/rating/all/byPublicId";
    }
    public static String getDatasetAllRating(String publicDSId) {
      return "private/rating/dataset/" + publicDSId + "/all";
    }
    
    public static String getDatasetUserRating(String publicCId, String publicDSId) {
      return "private/rating/cluster/" + publicCId + "/dataset/" + publicDSId + "/user";
    }
    
    public static String addRating(String publicCId, String publicDSId) {
      return "private/rating/cluster/" + publicCId + "/dataset/" + publicDSId + "/add";
    }
  }
  
  public static class CommentService {
    public static String getDatasetAllComments(String publicDSId) {
      return "private/comment/dataset/" + publicDSId + "/all";
    }
    
    public static String addComment(String publicCId, String publicDSId) {
      return "private/comment/cluster/" + publicCId + "/dataset/" + publicDSId + "/add";
    }
    
    public static String updateComment(String publicCId, String publicDSId, Integer commentId) {
      return "private/comment/cluster/" + publicCId + "/dataset/" + publicDSId + "/update/" + commentId; 
    }
    
    public static String removeComment(String publicCId, String publicDSId, Integer commentId) {
      return "private/comment/cluster/" + publicCId + "/dataset/" + publicDSId + "/delete/" + commentId; 
    }
    
    public static String reportComment(String publicCId, String publicDSId, Integer commentId) {
      return "private/comment/cluster/" + publicCId + "/dataset/" + publicDSId + "/report/" + commentId;
    }
  }

  public static interface UserFunc<C extends Object> {

    public C perform() throws ThirdPartyException;
  }
}
