package org.hl7.fhir.dstu3.metamodel;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.dstu3.formats.IParser.OutputStyle;
import org.hl7.fhir.dstu3.formats.RdfGenerator;
import org.hl7.fhir.dstu3.formats.RdfGenerator.Complex;
import org.hl7.fhir.dstu3.formats.RdfGenerator.Section;
import org.hl7.fhir.dstu3.formats.RdfGenerator.Subject;
import org.hl7.fhir.dstu3.utils.IWorkerContext;
import org.hl7.fhir.utilities.Utilities;

public class TurtleParser extends ParserBase {

  private String base;
  
  public TurtleParser(IWorkerContext context, boolean check) {
    super(context, check);
  }
  @Override
  public Element parse(InputStream stream) throws Exception {
    throw new NotImplementedException("not done yet");
  }
  @Override
  public void compose(Element e, OutputStream stream, OutputStyle style, String base) throws Exception {
    this.base = base;
    
		RdfGenerator ttl = new RdfGenerator(stream);
		//      ttl.setFormat(FFormat);
		ttl.prefix("fhir", "http://hl7.org/fhir/");
		ttl.prefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		ttl.prefix("owl", "http://www.w3.org/2002/07/owl#");
		
		Section section = ttl.section("resource");
		Subject subject;
		String id = e.getChildValue("id");
		if (base != null && id != null) 
			subject = section.triple("<"+base+"/"+e.getType()+"/"+id+">", "a", "fhir:"+e.getType());
		else
		  subject = section.triple("_", "a", "fhir:"+e.getType());
		subject.predicate("fhir:nodeRole", "treeRoot");

		for (Element child : e.getChildren()) {
			composeElement(subject, child);
		}
		ttl.commit(false);
  }
  
  protected void decorateReference(Complex t, Element coding) {
    String ref = coding.getChildValue("reference");
    if (ref != null && (ref.startsWith("http://") || ref.startsWith("https://")))
      t.predicate("fhir:reference", "<"+ref+">");
    else if (base != null && ref != null && ref.contains("/")) {
      t.predicate("fhir:reference", "<"+base+"/"+ref+">");
    }
  }
  
	protected void decorateCoding(Complex t, Element coding) {
		String system = coding.getChildValue("system");
		String code = coding.getChildValue("code");
		
		if (system == null)
			return;
		if ("http://snomed.info/sct".equals(system)) {
			t.prefix("sct", "http://snomed.info/sct/");
			t.predicate("a", "sct:"+code);
		} else if ("http://loinc.org".equals(system)) {
			t.prefix("loinc", "http://loinc.org/owl#");
			t.predicate("a", "loinc:"+code);
		}  
	}

	private void decorateCodeableConcept(Complex t, Element element) {
	  for (Element c : element.getChildren("coding")) {
	  	decorateCoding(t, c);
	  }
	}
	
	private void composeElement(Complex ctxt, Element element) {
		if ("xhtml".equals(element.getType())) // need to decide what to do with this
			return;
		String en = element.getProperty().getDefinition().getBase().getPath();
    if (en == null) 
      en = element.getProperty().getDefinition().getPath();
		boolean doType = false;
			if (en.endsWith("[x]")) {
				en = en.substring(0, en.length()-3);
				doType = true;				
			}
	   if (doType || element.getProperty().getDefinition().getType().size() > 1)
	     en = en + Utilities.capitalize(element.getType());

	  Complex t = ctxt.predicate("fhir:"+en);
	  if (element.hasValue())
	  	t.predicate("fhir:value", ttlLiteral(element.getValue()));
	  if (element.hasIndex())
	  	t.predicate("fhir:index", Integer.toString(element.getIndex()));

	  if ("Coding".equals(element.getType()))
	  	decorateCoding(t, element);
	  if ("CodeableConcept".equals(element.getType()))
	  	decorateCodeableConcept(t, element);
    if ("Reference".equals(element.getType()))
      decorateReference(t, element);
	  		
		for (Element child : element.getChildren()) {
			composeElement(t, child);
		}
	}
	
	protected String ttlLiteral(String value) {
		return "\"" +RdfGenerator.escape(value, true) + "\"";
	}

}
