// Copyright (C) 2009 - 2013 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.scriptengine.jython.instrumentation.dcr;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.grinder.script.NonInstrumentableTypeException;
import net.grinder.scriptengine.DCRContext;
import net.grinder.scriptengine.Recorder;
import net.grinder.util.weave.ParameterSource;
import net.grinder.util.weave.WeavingException;

import org.python.core.PyClass;
import org.python.core.PyFunction;
import org.python.core.PyInstance;
import org.python.core.PyMethod;
import org.python.core.PyObject;
import org.python.core.PyProxy;
import org.python.core.PyReflectedFunction;
import org.python.core.ThreadState;


/**
 * DCR instrumenter for Jython 2.5+.
 *
 * @author Philip Aston
 */
public final class Jython25Instrumenter extends AbstractJythonDCRInstrumenter {

  private final Transformer<PyInstance> m_pyInstanceTransformer;
  private final Transformer<PyFunction> m_pyFunctionTransformer;
  private final Transformer<PyProxy> m_pyProxyTransformer;
  private final Transformer<PyClass> m_pyClassTransformer;

  /**
   * Constructor.
   *
   * @param context The DCR context.
   * @throws WeavingException If the available version of Jython is
   *  incompatible with this instrumenter.
   */
  public Jython25Instrumenter(final DCRContext context)
    throws WeavingException  {

    super(context);

    try {
      final List<Method> methodsForPyFunction = new ArrayList<Method>();

      for (final Method method : PyFunction.class.getDeclaredMethods()) {
        // Roughly identify the fundamental __call__ methods, i.e. those
        // that call the actual func_code.
        if (("__call__".equals(method.getName()) ||
             // Add function__call__ for refactoring in Jython 2.5.2.
             "function___call__".equals(method.getName())) &&
            method.getParameterTypes().length >= 1 &&
            method.getParameterTypes()[0] == ThreadState.class) {
          methodsForPyFunction.add(method);
        }
      }

      assertAtLeastOneMethod(methodsForPyFunction);

      m_pyFunctionTransformer = new Transformer<PyFunction>() {
          @Override
          public void transform(final Recorder recorder,
                                final PyFunction target)
            throws NonInstrumentableTypeException {

            for (final Method method : methodsForPyFunction) {
              context.add(ParameterSource.FIRST_PARAMETER,
                          target,
                          method,
                          recorder);
            }
          }
        };

      final List<Method> methodsForPyInstance = new ArrayList<Method>();

      for (final Method method : PyFunction.class.getDeclaredMethods()) {
        // Here we're finding the fundamental __call__ methods that also
        // take an instance argument.
        if ("__call__".equals(method.getName()) &&
            method.getParameterTypes().length >= 2 &&
            method.getParameterTypes()[0] == ThreadState.class &&
            method.getParameterTypes()[1] == PyObject.class) {
          methodsForPyInstance.add(method);
        }
      }

      assertAtLeastOneMethod(methodsForPyInstance);

      m_pyInstanceTransformer = new Transformer<PyInstance>() {
          @Override
          public void transform(final Recorder recorder,
                                final PyInstance target)
            throws NonInstrumentableTypeException {

            for (final Method method : methodsForPyInstance) {
              context.add(ParameterSource.THIRD_PARAMETER,
                          target,
                          method,
                          recorder);
            }
          }
        };

      final List<Method> methodsForPyMethod = new ArrayList<Method>();

      for (final Method method : PyMethod.class.getDeclaredMethods()) {
        // Roughly identify the fundamental __call__ methods, i.e. those
        // that call the actual func_code.
        if (("__call__".equals(method.getName()) ||
             // Add instancemethod___call__ for refactoring in Jython 2.5.2.
             "instancemethod___call__".equals(method.getName())) &&
            method.getParameterTypes().length >= 1 &&
            method.getParameterTypes()[0] == ThreadState.class) {
          methodsForPyMethod.add(method);
        }
      }

      assertAtLeastOneMethod(methodsForPyMethod);

      final Method pyReflectedFunctionCall =
        PyReflectedFunction.class.getDeclaredMethod("__call__",
                                                    PyObject.class,
                                                    PyObject[].class,
                                                    String[].class);

      // PyProxy is used for Jython objects that extend a Java class.
      // We can't just use the Java wrapping, since then we'd miss the
      // Jython methods.

      // Need to look up this method dynamically, the return type differs
      // between 2.2 and 2.5.
      final Method pyProxyPyInstanceMethod =
        PyProxy.class.getDeclaredMethod("_getPyInstance");

      m_pyProxyTransformer = new Transformer<PyProxy>() {
          @Override
          public void transform(final Recorder recorder, final PyProxy target)
            throws NonInstrumentableTypeException {
            final PyObject pyInstance;

            try {
              pyInstance = (PyObject) pyProxyPyInstanceMethod.invoke(target);
            }
            catch (final Exception e) {
              throw new NonInstrumentableTypeException(
                "Could not call _getPyInstance", e);
            }

            for (final Method method : methodsForPyInstance) {
              context.add(ParameterSource.THIRD_PARAMETER,
                          pyInstance,
                          method,
                          recorder);
            }

            context.add(ParameterSource.SECOND_PARAMETER,
                        pyInstance,
                        pyReflectedFunctionCall,
                        recorder);
          }
        };

      final Method pyClassCall =
        PyClass.class.getDeclaredMethod("__call__",
                                        PyObject[].class,
                                        String[].class);

      m_pyClassTransformer = new Transformer<PyClass>() {
          @Override
          public void transform(final Recorder recorder, final PyClass target)
            throws NonInstrumentableTypeException {
            context.add(ParameterSource.FIRST_PARAMETER,
                        target,
                        pyClassCall,
                        recorder);
          }
        };
    }
    catch (final NoSuchMethodException e) {
      throw new WeavingException("Jython 2.5 not found", e);
    }
  }

  private static void assertAtLeastOneMethod(final List<Method> methods)
    throws WeavingException {
    if (methods.size() == 0) {
      throw new WeavingException("Jython 2.5 not found");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override public String getDescription() {
    return "byte code transforming instrumenter for Jython 2.5";
  }

  private interface Transformer<T> {
    void transform(Recorder recorder, T target)
      throws NonInstrumentableTypeException;
  }

  /**
   * {@inheritDoc}
   */
  @Override protected void transform(final Recorder recorder,
                                     final PyInstance target)
    throws NonInstrumentableTypeException {
    m_pyInstanceTransformer.transform(recorder, target);
  }

  /**
   * {@inheritDoc}
   */
  @Override protected void transform(final Recorder recorder,
                                     final PyFunction target)
    throws NonInstrumentableTypeException {
    m_pyFunctionTransformer.transform(recorder, target);
  }

  /**
   * {@inheritDoc}
   */
  @Override protected void transform(final Recorder recorder,
                                     final PyClass target)
    throws NonInstrumentableTypeException {
    m_pyClassTransformer.transform(recorder, target);
  }

  /**
   * {@inheritDoc}
   */
  @Override protected void transform(final Recorder recorder,
                                     final PyProxy target)
    throws NonInstrumentableTypeException {
    m_pyProxyTransformer.transform(recorder, target);
  }

  /**
   * {@inheritDoc}
   */
  @Override protected void transform(final Recorder recorder,
                                     final PyMethod target)
    throws NonInstrumentableTypeException {

    // PyMethod is a wrapper around a callable. Sometimes Jython bypasses
    // the PyMethod (e.g. dispatch of self.foo() calls). Sometimes there
    // are multiple PyMethods that refer to the same callable.

    // In the common case, the callable is a PyFunction wrapping some PyCode.
    // Experimentation shows that there'll be  a single PyFunction. However,
    // there's nothing that forces this to be true - some code path might
    // create a different PyFunction referring to the same code. Also, we must
    // cope with other types of callable. I guess I could identify
    // PyFunction's and dispatch on their im_code should this become an issue.

    final PyObject theFunc = func(target);
    final PyObject theSelf = self(target);

    if (theSelf == null) {
      // Unbound method.
      instrumentPublicMethodsByName(theFunc,
                                    "__call__",
                                    recorder,
                                    false);
    }
    else {
      instrumentPublicMethodsByName(theFunc.getClass(),
                                    "__call__",
                                    ParameterSource.FIRST_PARAMETER,
                                    theFunc,
                                    ParameterSource.THIRD_PARAMETER,
                                    theSelf,
                                    recorder,
                                    false);
    }
  }
}
