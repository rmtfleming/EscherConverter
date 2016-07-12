package edu.ucsd.sbrg.escher.converters;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import edu.ucsd.sbrg.escher.model.*;
import edu.ucsd.sbrg.escher.model.Point;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.ext.layout.*;
import org.sbml.jsbml.util.ResourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Created by deveshkhandelwal on 20/06/16.
 */
public class SBML2Escher {

  /**
   * A {@link java.util.logging.Logger} for this class.
   */
  private static final Logger         logger = Logger.getLogger(SBGN2Escher.class.getName());
  /**
   * Localization support.
   */
  public static final  ResourceBundle bundle = ResourceManager.getBundle("Strings");
  protected List<EscherMap> escherMaps;
  protected SBMLDocument    document;
  protected List<Layout>    layouts;

  public SBML2Escher() {
    escherMaps = new ArrayList<>();
  }


  public List<EscherMap> convert(SBMLDocument document) {
    this.document = document;

    layouts = ((LayoutModelPlugin)document.getModel().getPlugin(LayoutConstants.shortLabel)).getListOfLayouts();

    layouts.forEach((layout) -> {

      EscherMap map = new EscherMap();

      map.setCanvas(addCanvasInfo(layouts.get(0)));
      map.setDescription(bundle.getString("default_description"));
      map.setId(HexBin.encode(layouts.get(0).toString().getBytes()));

      layout.getListOfTextGlyphs().forEach(tG -> {
//        map.addTextLabel(createTextLabel(tG));
      });

      layout.getListOfSpeciesGlyphs().forEach((sG) -> {
        map.addNode(createNode(sG));
      });

      layout.getListOfReactionGlyphs().forEach((rG -> {
        map.addNode(createMidmarker(rG));
      }));

      layout.getListOfReactionGlyphs().forEach((rG) -> {
        rG.getListOfSpeciesReferenceGlyphs().forEach((sRG) -> {
          createMultiMarkers(sRG).forEach(m -> map.addNode(m));
        });
      });

      layout.getListOfReactionGlyphs().forEach((rG) -> {
        map.addReaction(createReaction(rG));
      });

      layout.getListOfReactionGlyphs().forEach((rG) -> {
        rG.getListOfSpeciesReferenceGlyphs().forEach((sRG -> {
          createSegments(sRG, rG).forEach(s -> map.getReaction("" + (rG.getId().hashCode() &
              0xfffffff)).addSegment(s));
        }));
      });

      // Setting node_is_primary of nodes.
      layout.getListOfReactionGlyphs().forEach(rG -> {
        rG.getListOfSpeciesReferenceGlyphs().forEach(sRG -> {
          if (sRG.getSpeciesReferenceRole() == SpeciesReferenceRole.PRODUCT ||
              sRG.getSpeciesReferenceRole() == SpeciesReferenceRole.SUBSTRATE) {
            map.getNode(sRG.getSpeciesGlyph()).setPrimary(true);
          }
          else if (sRG.getSpeciesReferenceRole() == SpeciesReferenceRole.SIDEPRODUCT ||
              sRG.getSpeciesReferenceRole() == SpeciesReferenceRole.SIDESUBSTRATE) {
            map.getNode(sRG.getSpeciesGlyph()).setPrimary(false);
          }
        });
      });

      escherMaps.add(map);
    });

    return escherMaps;
  }


  protected Canvas addCanvasInfo(Layout layout) {
    Canvas canvas = new Canvas();

    canvas.setX(Double.valueOf(bundle.getString("default_canvas_x")));
    canvas.setY(Double.valueOf(bundle.getString("default_canvas_y")));

    if (layout.getDimensions() == null) {
      canvas.setHeight(Double.valueOf(bundle.getString("default_canvas_height")));
      canvas.setWidth(Double.valueOf(bundle.getString("default_canvas_width")));
    }
    else {
      canvas.setHeight(layout.getDimensions().getHeight());
      canvas.setWidth(layout.getDimensions().getWidth());
    }

    return canvas;
  }


  protected TextLabel createTextLabel(TextGlyph textGlyph) {
    TextLabel textLabel = new TextLabel();

    if (textGlyph.getId() == null || textGlyph.getId().isEmpty()) {
      // TODO: Log about generating an Id.
      textLabel.setId("" + (textGlyph.hashCode() & 0xfffffff));
    }
    else {
      textLabel.setId(textGlyph.getId());
    }

    if (textGlyph.getText() == null || textGlyph.getText().isEmpty()) {
      // TODO: Log about no text, so ignoring text label.
      ;
    }
    else {
      textLabel.setText(textGlyph.getText());
    }

    textLabel.setX(textGlyph.getBoundingBox().getPosition().getX());
    textLabel.setY(textGlyph.getBoundingBox().getPosition().getY());

    return textLabel;
  }


  protected Node createNode(SpeciesGlyph speciesGlyph) {
    Node node = new Node();

    node.setType(Node.Type.metabolite);
    node.setId(speciesGlyph.getId());
    node.setBiggId(speciesGlyph.getSpecies());
    node.setName(speciesGlyph.getSpeciesInstance().getName());
    node.setX(speciesGlyph.getBoundingBox().getPosition().x());
    node.setY(speciesGlyph.getBoundingBox().getPosition().y());
    node.setLabelX(speciesGlyph.getBoundingBox().getPosition().x() +
                    speciesGlyph.getBoundingBox().getDimensions().getWidth());
    node.setLabelY(speciesGlyph.getBoundingBox().getPosition().y() +
                    speciesGlyph.getBoundingBox().getDimensions().getHeight());

    // TODO: Find out if node is primary by either role or SBO term.
    node.setPrimary(true);

    return node;
  }


  protected Node createMidmarker(ReactionGlyph reactionGlyph) {
    Node node = new Node();

    node.setId(reactionGlyph.getId());
    node.setType(Node.Type.midmarker);

    Point point = new Point();
    if (reactionGlyph.getBoundingBox() != null) {
      point.setX(reactionGlyph.getBoundingBox().getPosition().getX() + (0.5 * reactionGlyph
          .getBoundingBox().getDimensions().getWidth()));
      point.setY(reactionGlyph.getBoundingBox().getPosition().getY() + (0.5 * reactionGlyph
          .getBoundingBox().getDimensions().getHeight()));
    }
    else {
      point.setX(0.5 * (reactionGlyph.getCurve()
                                     .getCurveSegment(0)
                                     .getStart()
                                     .x() + reactionGlyph.getCurve()
                                                         .getCurveSegment(reactionGlyph.getCurve()
                                                                                       .getCurveSegmentCount()-1)
                                                         .getStart().x()));
      point.setY(0.5 * (reactionGlyph.getCurve()
                                     .getCurveSegment(0)
                                     .getStart()
                                     .y() + reactionGlyph.getCurve()
                                                         .getCurveSegment(reactionGlyph.getCurve()
                                                                                       .getCurveSegmentCount()-1)
                                                         .getStart().y()));
    }

    node.setX(point.getX());
    node.setY(point.getY());

    return node;
  }


  protected List<Node> createMultiMarkers(SpeciesReferenceGlyph sRG) {
    List<Node> multiMarkers = new ArrayList<>();

    Node node;
    List<CurveSegment> cSs = sRG.getCurve().getListOfCurveSegments();
    for (int i = 0; i < (cSs.size()-1); i++) {
      node = new Node();

      node.setId(sRG.getSpeciesReference() + ".M" + (i+1));
      node.setType(Node.Type.multimarker);

      node.setX(midPoint(cSs.get(i).getEnd().getX(), cSs.get(i+1).getStart().getX()));
      node.setY(midPoint(cSs.get(i).getEnd().getY(), cSs.get(i+1).getStart().getY()));

      multiMarkers.add(node);
    }

    return multiMarkers;
  }


  protected EscherReaction createReaction(ReactionGlyph reactionGlyph) {
    EscherReaction reaction = new EscherReaction();

    reaction.setName(reactionGlyph.getReactionInstance().getName());
    reaction.setId("" + (reactionGlyph.getId().hashCode() & 0xfffffff));
    reaction.setBiggId(reactionGlyph.getReactionInstance().getId());

    Point point = new Point();
    if (reactionGlyph.getBoundingBox() != null) {
      point.setX(reactionGlyph.getBoundingBox().getPosition().getX() + (0.5 * reactionGlyph
          .getBoundingBox().getDimensions().getWidth()));
      point.setY(reactionGlyph.getBoundingBox().getPosition().getY() + (0.5 * reactionGlyph
          .getBoundingBox().getDimensions().getHeight()));
    }
    else {
      point.setX(0.5 * (reactionGlyph.getCurve()
                              .getCurveSegment(0)
                              .getStart()
                              .x() + reactionGlyph.getCurve()
                                                  .getCurveSegment(reactionGlyph.getCurve()
                                                                                .getCurveSegmentCount()-1)
                                                  .getStart().x()));
      point.setY(0.5 * (reactionGlyph.getCurve()
                                     .getCurveSegment(0)
                                     .getStart()
                                     .y() + reactionGlyph.getCurve()
                                                         .getCurveSegment(reactionGlyph.getCurve()
                                                                                       .getCurveSegmentCount()-1)
                                                         .getStart().y()));
    }
    reaction.setLabelX(point.getX());
    reaction.setLabelY(point.getY());

    // Add metabolites.
    ((Reaction) reactionGlyph.getReactionInstance()).getListOfProducts().forEach((p) -> {
      reaction.addMetabolite(createMetabolite(p));
    });

    ((Reaction) reactionGlyph.getReactionInstance()).getListOfReactants().forEach((r) -> {
      r.setStoichiometry(-1 * r.getStoichiometry());
      reaction.addMetabolite(createMetabolite(r));
    });

    // TODO: Think of what to do about genes.

    // TODO: Add segments.

    return reaction;
  }


  protected List<Segment> createSegments(SpeciesReferenceGlyph sRG, ReactionGlyph rG) {
    List<Segment> segments = new ArrayList<>();
    Segment segment = new Segment();
    List<CurveSegment> cSs = sRG.getCurve().getListOfCurveSegments();

    segment.setId(sRG.getId() + ".S" + 0);
    segment.setFromNodeId(sRG.getSpeciesGlyph());
    for (int i = 0; i < (cSs.size()-1); i++) {
      segment.setToNodeId(sRG.getSpeciesReference() + ".M" + (i+1));

      if (cSs.get(i).isCubicBezier()) {
        CubicBezier cB = (CubicBezier) cSs.get(i);

        org.sbml.jsbml.ext.layout.Point point;

        point = cB.getBasePoint1();
        segment.setBasePoint1(new Point(point.x(), point.y()));
        point = cB.getBasePoint2();
        segment.setBasePoint2(new Point(point.x(), point.y()));
      }
      segments.add(segment);

      segment = new Segment();

      segment.setId(sRG.getId() + ".S" + (i+1));
      segment.setFromNodeId(sRG.getSpeciesReference() + ".M" + (i+1));
    }

    segment.setToNodeId(rG.getId());

    if (cSs.get(cSs.size()-1).isCubicBezier()) {
      CubicBezier cB = (CubicBezier) cSs.get(cSs.size()-1);

      org.sbml.jsbml.ext.layout.Point point;

      point = cB.getBasePoint1();
      segment.setBasePoint1(new Point(point.x(), point.y()));
      point = cB.getBasePoint2();
      segment.setBasePoint2(new Point(point.x(), point.y()));
    }

    segments.add(segment);
    
    return segments;
  }


  protected Metabolite createMetabolite(SpeciesReference speciesReference) {
    Metabolite metabolite = new Metabolite();

    metabolite.setId(speciesReference.getSpecies());
    metabolite.setCoefficient(speciesReference.getCalculatedStoichiometry());

    return metabolite;
  }


  protected double midPoint(double d1, double d2) {
    return (d1 + d2)/2;
  }


  protected void postProcessMap(EscherMap map) {

  }

}
