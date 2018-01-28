import de.prob.Main
import de.prob.scripting.Api
import de.prob.statespace.State
import de.prob.statespace.StateSpace
import de.prob.statespace.Transition

import cucumber.api.*
import static cucumber.api.groovy.EN.*
import static cucumber.api.groovy.Hooks.*

def quoted = /(?:"|')([^"']*)(?:"|')/


// Debug

And(~/^printState$/) { ->
    println(space.printState(state))
}


// Event-B

// Setup constants (optionally with the given constants constraints) and initialize the machine.
Given(~/^machine(?: with ${quoted})?$/) {
    String formula ->   // Parameter formula is optional
    setupConstantsInitialiseMachine(formula)
}

// Fire the given event (optionally with the given parameters constraints).
When(~/^fire event ${quoted}(?: with ${quoted})?$/) {
    String eventName, String formula ->   // Parameter formula is optional
    fireEvent(eventName, formula)
}

// Check if the given event (optionally with the given parameters constraints) is enabled.
Then(~/^event ${quoted}(?: with ${quoted})? is enabled$/) {
    String eventName, String formula ->   // Parameter formula is optional
    assert true == isEventEnabled(eventName, formula)
}

// Check if the given event (optionally with the given parameters constraints) is disabled.
Then(~/^event ${quoted}(?: with ${quoted})? is disabled$/) {
    String eventName, String formula ->   // Parameter formula is optional
    assert true == isEventDisabled(eventName, formula)
}

// Check if the given formula evaluates to the given value.
Then(~/^formula ${quoted} is (FALSE|TRUE)$/) {
    String formula, String value ->
    assert true == isFormula(formula, value)
}


// iUML-B State Machines

// Preset the given state machine for subsequent steps.
Given(~/^state machine ${quoted}?$/) {
    String smId ->
    givenSmId = smId
}

// Trigger the given transition (optionally with the given parameters constraints) of the given or preset state machine.
When(~/^trigger transition ${quoted}(?: with ${quoted})?(?: for state machine ${quoted})?$/) {
    String transName, String formula, String smId ->   // Parameters formula and smId are optional
    triggerTransition(transName, smId, formula)
}

// Check, if the given transition (optionally with the given parameters constraints) of the given or preset state machine is enabled.
Then(~/^transition ${quoted}(?: with ${quoted})? is enabled(?: for state machine ${quoted})?$/) {
    String transName, String formula, String smId ->   // Parameters formula and smId are optional
    assert true == isTransitionEnabled(transName, smId, formula)
}

// Check, if the given transition (optionally with the given parameters constraints) of the given or preset state machine is disabled.
Then(~/^transition ${quoted}(?: with ${quoted})? is disabled(?: for state machine ${quoted})?$/) {
    String transName, String formula, String smId ->   // Parameters formula and smId are optional
    assert true == isTransitionDisabled(transName, smId, formula)
}

// Check, if the given or preset state machine is or is not in the given state.
Then(~/^(?:state machine ${quoted} )?is( not)? in state ${quoted}$/) {
    String smId, String not, String stateName ->   // Parameters smId and not are optional
    assert true == isInState(smId, stateName, not != null)
}


// iUML-B Class Diagrams

// Preset the given class instance for subsequent steps.
Given(~/^class ${quoted}?$/) {
    String classId ->
    givenClassId = classId
}

// Call the given method (optionally with the given parameters constraints) of the preset or given classs instance.
When(~/^call method ${quoted}(?: with ${quoted})?(?: for class instance ${quoted})?$/) {
    String methodName, String formula, String classId ->   // Parameter formula and classId are optional
    callMethod(methodName, classId, formula)
}

// Check, if the given method (optionally with the given parameters constraints) of the preset or given classs instance is enabled.
Then(~/^method ${quoted}(?: with ${quoted})? is enabled(?: for class instance ${quoted})?$/) {
    String methodName, String formula, String classId ->   // Parameter formula and classId are optional
    assert true == isMethodEnabled(methodName, classId, formula)
}

// Check, if the given method (optionally with the given parameters constraints) of the preset or given classs instance is disabled.
Then(~/^method ${quoted}(?: with ${quoted})? is disabled(?: for class instance ${quoted})?$/) {
    String methodName, String formula, String classId ->   // Parameter formula and classId are optional
    assert true == isMethodDisabled(methodName, classId, formula)
}

// Check, if the given attribute of the given class instance has or has not the given value.
Then(~/^attribute ${quoted}(?: of class instance ${quoted})? is( not)? ${quoted}$/) {
    String attrName, String classId, String not, String value ->   // Parameters classId and not are optional
    assert true == isAttribute(classId, attrName, value, not != null)
}


Before() {
    givenSmId = null
    givenClassId = null
}

After() { }

World() {
    World.instance
}

@Singleton
public class World {

    Api api = Main.getInjector().getInstance(Api.class)
    String eventb = System.getProperty("eventb")
    StateSpace space = api.eventb_load(eventb) 
    State state

    void setupConstantsInitialiseMachine(String formula = null) {
        state = space.getRoot()
        Transition trans = findUniqueTransition("\$setup_constants", formula)
        if (trans != null) {
            state = trans.getDestination()
        }
        trans = findUniqueTransition("\$initialise_machine", null)
        if (trans != null) {
            state = trans.getDestination()
        }
    }

    void fireEvent(String eventName, String formula = null) {
        Transition trans = findUniqueTransition(eventName, formula)
        state = trans.getDestination()
    }

    boolean isEventEnabled(String eventName, String formula = null) {
        Transition trans = findUniqueTransition(eventName, formula)
        return trans != null
    }

    boolean isEventDisabled(String eventName, String formula = null) {
        Transition trans = findUniqueTransition(eventName, formula)
        return trans == null
    }

    // Value can be a literal FALSE or TRUE
    boolean isFormula(String formula, String value) {
        state.eval(formula).toString() == "${value}"
    }

    // Helper methods

    private Transition findUniqueTransition(String eventName, String formula) {
        def form = formula ? [formula] : []
        def transitions = state.findTransitions(eventName, form, 2)
        def transitionCount = transitions.size()
        assert transitionCount <= 1 : "Transition " + eventName + " is not unique"
        if (!transitions.isEmpty()) {
            return transitions[0]
        }
        return null
    }

    private String mergeFormulas(String formula1, String formula2) {
        return formula2 != null ? "(${formula1}) & (${formula2})" : formula1
    }
    
    // iUML-B
    def cdNodes = [:]
    def smNodes = [:]
    def machineNode = iumlb_load()

    String givenSmId = null
    String givenClassId = null

    void triggerTransition(transName, smId, String formula = null) {
        def (smName, smInst) = getStateMachine(smId)
        String selfName = getSelfName(smName, smInst)
        if (selfName != null) {
            fireEvent(transName, mergeFormulas("${selfName} = ${smInst}", formula))
        } else {
            fireEvent(transName, formula)
        }
    }

    boolean isTransitionEnabled(transName, smId, String formula = null) {
        def (smName, smInst) = getStateMachine(smId)
        String selfName = getSelfName(smName, smInst)
        if (selfName != null) {
            return isEventEnabled(transName, mergeFormulas("${selfName} = ${smInst}", formula))
        } else {
            return isEventEnabled(transName, formula)
        }
    }

    boolean isTransitionDisabled(transName, smId, String formula = null) {
        def (smName, smInst) = getStateMachine(smId)
        String selfName = getSelfName(smName, smInst)
        if (selfName != null) {
            return isEventDisabled(transName, mergeFormulas("${selfName} = ${smInst}", formula))
        } else {
            return isEventDisabled(transName, formula)
        }
    }

    boolean isInState(String smId, String stateName, boolean not) {
        def (smName, smInst) = getStateMachine(smId)
        String ref = not ? "FALSE" : "TRUE"
        assert hasState(smName, stateName) : "Invalid state ${stateName} for state machine ${smName}"
        String tr = smNodes[smName][0].attribute("translation")
        String selfName = getSelfName(smName, smInst)
        switch(tr) {
            case null: // MULTIVAR
                if (selfName != null) {
                    return isFormula("${smInst} ∈ ${stateName}", ref)
                } else {
                    return isFormula("${stateName} = TRUE", ref)
                }
                break;
            case "SINGLEVAR":
                if (selfName != null) {
                    return isFormula("${smName}(${smInst}) = ${stateName}", ref)
                } else {
                    return isFormula("${smName} = ${stateName}", ref)
                }
                break;
            default:
                assert false: "Unsupported translation ${tr} for state machine ${smName}"
                break;
        }
    }

    void callMethod(methodName, classId, String formula = null) {
        def (className, classInst) = getClassInstance(classId)
        String selfName = getClassSelfName(className, classInst)
        fireEvent(methodName, mergeFormulas("${selfName} = ${classInst}", formula))
    }

    boolean isMethodEnabled(methodName, classId, String formula = null) {
        def (className, classInst) = getClassInstance(classId)
        String selfName = getClassSelfName(className, classInst)
        return isEventEnabled(methodName, mergeFormulas("${selfName} = ${classInst}", formula))
    }

    boolean isMethodDisabled(methodName, classId, String formula = null) {
        def (className, classInst) = getClassInstance(classId)
        String selfName = getClassSelfName(className, classInst)
        return isEventDisabled(methodName, mergeFormulas("${selfName} = ${classInst}", formula))
    }

    boolean isAttribute(String classId, String attrName, String value, boolean not) {
        def (className, classInst) = getClassInstance(classId)
        String selfName = getClassSelfName(className, classInst)
        String ref = not ? "FALSE" : "TRUE"
        return isFormula("${attrName}(${classInst}) = ${value}", ref)
    }

    // Helper methods

    private getStateMachine(String smId) {
        def sm = smId != null ? smId : givenSmId
        assert sm != null : "Missing state machine"
        return sm.tokenize(':')
    }

    private getClassInstance(String classId) {
        def cl = classId != null ? classIdId : givenClassId
        assert cl != null : "Missing class instance"
        return cl.tokenize(':')
    }

    private iumlb_load() {
        XmlParser parser = new XmlParser()
        Node eventbNode = null // Parsed node corresponding to the tested refinement
        String machineName = eventb
        while (machineName != null) {
            Node machineNode = parser.parse(machineName)
            if (eventb == null) {
                eventbNode = machineNode
            }
            for (node in machineNode.children()) {
                if (node.name() == 'ac.soton.eventb.emf.core.extension.persistence.serialisedExtension') {
                    def eClassifier = node.attribute('ac.soton.eventb.emf.core.extension.persistence.eClassifier')
                    def ePackageURI = node.attribute('ac.soton.eventb.emf.core.extension.persistence.ePackageURI')
                    def serialized = node.attribute('ac.soton.eventb.emf.core.extension.persistence.serialised')
                    def serializedNode = parser.parseText(serialized)
                    def nodeName = serializedNode.attribute('name')
                    switch (eClassifier) {   // ePackageURI
                        case "Classdiagram": // http://soton.ac.uk/models/eventb/classdiagrams/2015
                            if (cdNodes[nodeName] != null) {
                                cdNodes[nodeName] << serializedNode
                            } else {
                                cdNodes[nodeName] = [serializedNode]
                            }
                            break;
                        case "Statemachine": // http://soton.ac.uk/models/eventb/statemachines/2014
                            if (smNodes[nodeName] != null) {
                                smNodes[nodeName] << serializedNode
                                } else {
                                smNodes[nodeName] = [serializedNode]
                            }
                            break;
                    }
                    // groovy.xml.XmlUtil.serialize(serializedNode, new FileWriter(nodeName + ".xml"))
                }
            }
            machineName = machineNode."org.eventb.core.refinesMachine"[0]?.attribute("org.eventb.core.target")
            if (machineName != null) {
                machineName += ".bum"
            }
        }
        return eventbNode
    }

    // Get the state machine self name or null if not lifted.
    private String getSelfName(String smName, String smInst) {
        String selfName = smNodes[smName][0].attribute("selfName")
        if (smInst != null) {
            // Lifted state machine, selfName must not be empty
            assert selfName != "" : "Superfluous instance for unlifted state machine ${smName}"
            return selfName
        } else {
            // Not lifted state machine, selfName must be empty
            assert selfName == "" : "Missing instance for lifted state machine ${smName}"
            return null
        }
    }

    // Get the class self name (instance must not be null).
    private String getClassSelfName(String className, String classsInst) {
        assert instance != null : "Missing instance for class ${className}"
        for (cdNode in cdNodes[className]) {
            for (classNode in cdNode."classes") {
                if (classNode.attribute("name") == className) {
                    String selfName = classNode.attribute("selfName")
                    if (selfName != null) {
                        return selfName
                    }
                }
            }
        }
        assert false : "Missing selfName for class ${className}"
    }

    // Check if the state machine has the particular state
    private boolean hasState(String smName, String stateName) {
        for (node in smNodes[smName][0].children()) {
            if ((node.name() == "nodes") && (node.attribute("name") == stateName)) {
                return true;
            }
        }
        return false;
    }
}