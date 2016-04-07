
result = instance:override(2)
-- Check that the returned object can be used as a TestB (previous method uses overridden return type)
result:testB()
