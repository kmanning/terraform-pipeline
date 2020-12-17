import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue
import static org.mockito.Mockito.inOrder
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.verifyNoMoreInteractions

import org.mockito.InOrder
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import de.bechte.junit.runners.context.HierarchicalContextRunner

@RunWith(HierarchicalContextRunner.class)
class HookPointTest {

    public class Constructor {
        @Test
        void setsHookName() {
            def hp = new HookPoint("foo")
            assertEquals(hp.getName(), "foo")
        }
    }

    public class IsConfigured {
        @Test
        void falseIfAllNull() {
            def hp = new HookPoint("foo")
            assertFalse(hp.isConfigured())
        }

        @Test
        void trueIfRunBefore() {
            def hp = new HookPoint("foo")
            hp.runBefore = 'bar'
            assertTrue(hp.isConfigured())
        }

        @Test
        void trueIfRunAfterOnSuccess() {
            def hp = new HookPoint("foo")
            hp.runAfterOnSuccess = 'bar'
            assertTrue(hp.isConfigured())
        }

        @Test
        void trueIfRunAfterOnFailure() {
            def hp = new HookPoint("foo")
            hp.runAfterOnFailure = 'bar'
            assertTrue(hp.isConfigured())
        }

        @Test
        void trueIfRunAfterAlways() {
            def hp = new HookPoint("foo")
            hp.runAfterAlways = 'bar'
            assertTrue(hp.isConfigured())
        }
    }

    public class GetClosure {
        @After
        public void reset() {
            Jenkinsfile.instance = null
            TerraformEnvironmentStage.reset()
            TerraformEnvironmentStageShellHookPlugin.reset()
        }

        @Test
        void runsAllConfiguredHooks() {
            def hp = new HookPoint("foo")
            hp.runBefore = 'run-before'
            hp.runAfterOnSuccess = 'after-success'
            hp.runAfterOnFailure = 'after-failure'
            hp.runAfterAlways = 'after-always'

            def dummyJenkinsfile = mock(DummyJenkinsfile.class)

            def passedClosure = { -> sh 'passedClosure' }
            passedClosure.delegate = dummyJenkinsfile

            def result = hp.getClosure()
            result.delegate = dummyJenkinsfile
            result.call(passedClosure)

            InOrder inOrder = inOrder(dummyJenkinsfile);
            inOrder.verify(dummyJenkinsfile, times(1)).sh('run-before')
            inOrder.verify(dummyJenkinsfile, times(1)).sh('passedClosure')
            inOrder.verify(dummyJenkinsfile, times(1)).sh('after-success')
            inOrder.verify(dummyJenkinsfile, times(1)).sh('after-always')
            inOrder.verifyNoMoreInteractions()
            verifyNoMoreInteractions(dummyJenkinsfile)
        }

        @Test
        @SuppressWarnings('EmptyCatchBlock')
        void runsHooksOnFailure() {
            def hp = new HookPoint("foo")
            hp.runBefore = 'run-before'
            hp.runAfterOnSuccess = 'after-success'
            hp.runAfterOnFailure = 'after-failure'
            hp.runAfterAlways = 'after-always'

            def dummyJenkinsfile = mock(DummyJenkinsfile.class)

            def passedClosure = { ->
                sh 'passedClosure'
                throw new Exception()
            }
            passedClosure.delegate = dummyJenkinsfile

            def result = hp.getClosure()
            result.delegate = dummyJenkinsfile

            try {
                result.call(passedClosure)
            } catch (Exception e) { }

            InOrder inOrder = inOrder(dummyJenkinsfile);
            inOrder.verify(dummyJenkinsfile, times(1)).sh('run-before')
            inOrder.verify(dummyJenkinsfile, times(1)).sh('passedClosure')
            inOrder.verify(dummyJenkinsfile, times(1)).sh('after-failure')
            inOrder.verify(dummyJenkinsfile, times(1)).sh('after-always')
            inOrder.verifyNoMoreInteractions()
            verifyNoMoreInteractions(dummyJenkinsfile)
        }

        @Test
        void runsOnlyBeforeHook() {
            def hp = new HookPoint("foo")
            hp.runBefore = 'run-before'

            def dummyJenkinsfile = mock(DummyJenkinsfile.class)

            def passedClosure = { -> sh 'passedClosure' }
            passedClosure.delegate = dummyJenkinsfile

            def result = hp.getClosure()
            result.delegate = dummyJenkinsfile
            result.call(passedClosure)

            InOrder inOrder = inOrder(dummyJenkinsfile);
            inOrder.verify(dummyJenkinsfile, times(1)).sh('run-before')
            inOrder.verify(dummyJenkinsfile, times(1)).sh('passedClosure')
            inOrder.verifyNoMoreInteractions()
            verifyNoMoreInteractions(dummyJenkinsfile)
        }

        @Test
        void runsOnlyAfterSuccessHook() {
            def hp = new HookPoint("foo")
            hp.runAfterOnSuccess = 'after-success'

            def dummyJenkinsfile = mock(DummyJenkinsfile.class)

            def passedClosure = { -> sh 'passedClosure' }
            passedClosure.delegate = dummyJenkinsfile

            def result = hp.getClosure()
            result.delegate = dummyJenkinsfile
            result.call(passedClosure)

            InOrder inOrder = inOrder(dummyJenkinsfile);
            inOrder.verify(dummyJenkinsfile, times(1)).sh('passedClosure')
            inOrder.verify(dummyJenkinsfile, times(1)).sh('after-success')
            inOrder.verifyNoMoreInteractions()
            verifyNoMoreInteractions(dummyJenkinsfile)
        }

        @Test
        void runsOnlyAfterAlwaysHook() {
            def hp = new HookPoint("foo")
            hp.runAfterAlways = 'after-always'

            def dummyJenkinsfile = mock(DummyJenkinsfile.class)

            def passedClosure = { -> sh 'passedClosure' }
            passedClosure.delegate = dummyJenkinsfile

            def result = hp.getClosure()
            result.delegate = dummyJenkinsfile
            result.call(passedClosure)

            InOrder inOrder = inOrder(dummyJenkinsfile);
            inOrder.verify(dummyJenkinsfile, times(1)).sh('passedClosure')
            inOrder.verify(dummyJenkinsfile, times(1)).sh('after-always')
            inOrder.verifyNoMoreInteractions()
            verifyNoMoreInteractions(dummyJenkinsfile)
        }

        @Test
        @SuppressWarnings('EmptyCatchBlock')
        void runsOnlyAfterAlwaysHookOnFailure() {
            def hp = new HookPoint("foo")
            hp.runAfterAlways = 'after-always'

            def dummyJenkinsfile = mock(DummyJenkinsfile.class)

            def passedClosure = { ->
                sh 'passedClosure'
                throw new Exception()
            }
            passedClosure.delegate = dummyJenkinsfile

            def result = hp.getClosure()
            result.delegate = dummyJenkinsfile
            try {
                result.call(passedClosure)
            } catch (Exception e) { }

            InOrder inOrder = inOrder(dummyJenkinsfile);
            inOrder.verify(dummyJenkinsfile, times(1)).sh('passedClosure')
            inOrder.verify(dummyJenkinsfile, times(1)).sh('after-always')
            inOrder.verifyNoMoreInteractions()
            verifyNoMoreInteractions(dummyJenkinsfile)
        }

        @Test
        @SuppressWarnings('EmptyCatchBlock')
        void runsOnlyAfterFailureHook() {
            def hp = new HookPoint("foo")
            hp.runAfterOnFailure = 'after-failure'

            def dummyJenkinsfile = mock(DummyJenkinsfile.class)

            def passedClosure = { ->
                sh 'passedClosure'
                throw new Exception()
            }
            passedClosure.delegate = dummyJenkinsfile

            def result = hp.getClosure()
            result.delegate = dummyJenkinsfile
            try {
                result.call(passedClosure)
            } catch (Exception e) { }

            InOrder inOrder = inOrder(dummyJenkinsfile);
            inOrder.verify(dummyJenkinsfile, times(1)).sh('passedClosure')
            inOrder.verify(dummyJenkinsfile, times(1)).sh('after-failure')
            inOrder.verifyNoMoreInteractions()
            verifyNoMoreInteractions(dummyJenkinsfile)
        }
    }
}
