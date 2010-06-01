#include "testify/testify.hpp"

using namespace Testify;

class MyTestCase : public TestCase
{
public:
        MyTestCase( )
        : TestCase( "MyTestCase" )
        {
                // Add/bind tests here
                // addTest( bind( &MyTestCase::test1, this ), "test1" );
        }


        /**
         * Add your actual functions here
         */
        /*void test1 ( void )
        {
            TESTIFY_ASSERT( 1 );
        }*/

};

static TestHook hook( new MyTestCase( ) );

#ifndef TESTIFY_IDE_SETUP
#define TESTIFY_IDE_SETUP
#include "testify/listenerhook.hpp"
#include "testify/idelistener.hpp"
static ListenerHook __h__( new IDEListener() );
#endif
