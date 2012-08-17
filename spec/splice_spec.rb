require 'candlepin_scenarios'

describe 'Splice-specific calls' do

  include CandlepinMethods
  include CandlepinScenarios

  def verify_installed_pids(consumer, product_ids)
    installed_ids = consumer['installedProducts'].collect { |p| p['productId'] }
    installed_ids.length.should == product_ids.length
    product_ids.each do |pid|
      installed_ids.should include(pid)
    end
  end

  it 'should get an entitlement cert based on installed products' do
    # setup products
    pid1 = random_string('product-id1')
    pid2 = random_string('product-id2')
    installed = [
        {'productId' => pid1, 'productName' => 'My Installed Product'},
        {'productId' => pid2, 'productName' => 'Another Installed Product'}]

    product1 = create_product(pid1, random_string('product1'))
    product2 = create_product(pid2, random_string('product2'))

    # NB: everything below should be just one or two API calls
    # make pools for what the RHIC says we have access to
    @cp.create_subscription('admin', product1.id)
    @cp.create_subscription('admin', product2.id)
    @cp.refresh_pools('admin')

    # create a consumer with installed products
    consumer = @cp.register('machine1', :system, nil, {}, nil, 'admin', [], installed)
    verify_installed_pids(consumer, [pid1, pid2])

    # consume!
    @consumer_cp = Candlepin.new(nil, nil, consumer.idCert.cert, consumer.idCert['key'])
    # see what we get back
    ents = @consumer_cp.consume_product()

    ents.size.should == 2

  end

  it 'should not get covered for products installed but not available' do
    pid1 = random_string('product-id1')
    pid2 = random_string('product-id2')
    installed = [
        {'productId' => pid1, 'productName' => 'My Installed Product'},
        {'productId' => pid2, 'productName' => 'Another Installed Product'}]
    consumer = @cp.register('machine1', :system, nil, {}, nil, 'admin', [], installed)
    verify_installed_pids(consumer, [pid1, pid2])

    product1 = create_product(pid1, random_string('product1'))
    #product2 = create_product(pid2, random_string('product2'))
    @cp.create_subscription('admin', product1.id)
    #@cp.create_subscription('admin', product2.id)
    @cp.refresh_pools('admin')

    @consumer_cp = Candlepin.new(nil, nil, consumer.idCert.cert, consumer.idCert['key'])
    ents = @consumer_cp.consume_product()

    ents.size.should == 1

  end
end

