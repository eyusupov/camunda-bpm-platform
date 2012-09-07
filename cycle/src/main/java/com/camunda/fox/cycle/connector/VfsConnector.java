package com.camunda.fox.cycle.connector;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;

import com.camunda.fox.cycle.api.connector.Connector;
import com.camunda.fox.cycle.api.connector.ConnectorNode;
import com.camunda.fox.cycle.api.connector.ConnectorNode.ConnectorNodeType;
import com.camunda.fox.cycle.api.connector.Secured;
import com.camunda.fox.cycle.entity.ConnectorConfiguration;
import com.camunda.fox.cycle.exception.CycleException;
import com.camunda.fox.cycle.exception.RepositoryException;

public class VfsConnector extends Connector {

  public static final String BASE_PATH_KEY = "BASE_PATH";
  public static final String DEFAULT_BASE_PATH = "file://" + System.getProperty("user.home") + File.separatorChar + "cycle" + File.separatorChar;
  
  private static Logger logger = Logger.getLogger(VfsConnector.class.getSimpleName());

  private String basePath;

  @Secured
  @Override
  public List<ConnectorNode> getChildren(ConnectorNode parent) {
    List<ConnectorNode> nodes = new ArrayList<ConnectorNode>();
    
    try {
      FileSystemManager fsManager = VFS.getManager();
      FileObject fileObject;
      
      fileObject = fsManager.resolveFile(basePath + parent.getId());

      if (fileObject.getType() == FileType.FILE) {
        return Collections.<ConnectorNode>emptyList();
      }
      
      FileObject[] children = fileObject.getChildren();
      
      for ( FileObject file : children )
      {
          String baseName = file.getName().getBaseName();
          ConnectorNode node = new ConnectorNode(parent.getId()+File.separatorChar+baseName, baseName, getId());
          if (file.getType() == FileType.FILE) {
            node.setType(ConnectorNodeType.FILE);
          } else {
            node.setType(ConnectorNodeType.FOLDER);
          }
          
          /**
           * it's not possible to get last modified date from symlinks
           */
          try {
            node.setLastModified(new Date(file.getContent().getLastModifiedTime()));
          }catch (Exception exception) {
            logger.fine("Could not set last modified time");
          }
          
          nodes.add(node);
      }
      
      // Sort
      Collections.sort(nodes, new ConnectorNodeComparator());
      return nodes; 
    } catch (FileSystemException e) {
      throw new CycleException(e);
    }
  }

  @Secured
  @Override
  public InputStream getContent(ConnectorNode node, ConnectorContentType type) {
    try {
      FileSystemManager fsManager = VFS.getManager();
      
      FileObject fileObject = fsManager.resolveFile(basePath + node.getId());

      if (fileObject.getType() != FileType.FILE) {
        throw new CycleException("Cannot get content of non-file node");
      }
      
      switch(type) {
      case PNG:
        FileObject pngfile = fsManager.resolveFile(basePath + getPngFileName(node.getId()));
        if (pngfile.exists()) {
          return pngfile.getContent().getInputStream(); 
        }else {
          return getClass().getClassLoader().getResourceAsStream("no-picture.png");
        }
      default:
        return fileObject.getContent().getInputStream(); 
      }
      
    } catch (FileSystemException e) {
      throw new CycleException(e);
    }
  }
  
  @Override
  public void init(ConnectorConfiguration config) {
    basePath = config.getProperties().get(BASE_PATH_KEY);
    
    // Load base path from system property, e.g. ${user.home} if it is passed
    if (basePath.matches("\\$\\{.*\\}")) {
      String systemProperty = basePath.substring(2, basePath.length() - 1);
      String systemPropertyValue = System.getProperty(basePath.substring(2, basePath.length() - 1));
      if (systemPropertyValue != null) {
        try {
          basePath = new File(systemPropertyValue).toURI().toString();
          logger.info("Loading base path from system property " + systemProperty + ": " + basePath);
        } catch (Exception e) {
          logger.log(Level.WARNING, "Could not read base path from system property "+  basePath);
          basePath = DEFAULT_BASE_PATH;
        }
      }
    }
  }

  @Secured
  @Override
  public ConnectorNode getRoot() {
    ConnectorNode rootNode = new ConnectorNode("/", "/");
    rootNode.setType(ConnectorNodeType.FOLDER);
    return rootNode;
  }

  @Secured
  @Override
  public void updateContent(ConnectorNode node, InputStream newContent)  throws Exception {
    FileSystemManager fsManager = VFS.getManager();
    FileObject fileObject;
    
    fileObject = fsManager.resolveFile(this.basePath + node.getId());
    
    if (fileObject.exists()) {
      if (fileObject.getType() != FileType.FILE) {
        throw new CycleException("Unable to update content of file '" + node.getLabel() + "': Assigned file is not a file.");
      }
      FileContent content = fileObject.getContent();
      IOUtils.copy(newContent, content.getOutputStream());
      content.close();
    } else {
      throw new RepositoryException("File '" + node.getLabel() + "' does not exist.");
    }
  }

  @Override
  public ConnectorNode getNode(String id) {
    try {
      FileSystemManager fsManager = VFS.getManager();
      FileObject fileObject;

      fileObject = fsManager.resolveFile(basePath + id);

      String baseName = fileObject.getName().getBaseName();
      ConnectorNode node = new ConnectorNode(id, baseName);

      if (fileObject.getType() == FileType.FILE) {
        node.setType(ConnectorNodeType.FILE);
      } else {
        node.setType(ConnectorNodeType.FOLDER);
      }
      
      return node;

    } catch (Exception e) {
      throw new CycleException(e);
    }

  }

  @Override
  public ConnectorNode createNode(String id, String label, ConnectorNodeType type) {
    try {
      FileSystemManager fsManager = VFS.getManager();
      FileObject fileObject;

      fileObject = fsManager.resolveFile(basePath + id);
      
      switch (type) {
        case FILE:
          fileObject.createFile();
          break;
        case FOLDER:
          fileObject.createFolder();
        default:
          throw new RuntimeException("Unsupported Node Type");
      }
      
      return new ConnectorNode(id, label, type);

    } catch (Exception e) {
      throw new CycleException(e);
    }
  }

  @Override
  public void deleteNode(String id) {
    try {
      FileSystemManager fsManager = VFS.getManager();
      FileObject fileObject;

      fileObject = fsManager.resolveFile(basePath + id);
      fileObject.delete();

    } catch (Exception e) {
      throw new CycleException(e);
    }
  }
  
  private String getPngFileName(String nodeId) {
    int pointIndex = nodeId.lastIndexOf(".");
    return nodeId.substring(0, pointIndex)+".png";
  }
}
